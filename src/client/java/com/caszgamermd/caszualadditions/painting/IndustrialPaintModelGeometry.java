package com.caszgamermd.caszualadditions.painting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Read the EXACT baked-model source elements and visible faces for each block
 * state. Collision/outline shapes are intentionally NOT used: a rack may
 * collide as a whole cube while its visible rails occupy only a few pixels.
 * Handles variants, multipart, directional model rotation and element
 * rotation from both vanilla and modded asset JSON files.
 */
public final class IndustrialPaintModelGeometry {
    private static final int LIMIT=384;
    /** Bit order: west/east, down/up, north/south. */
    public record Part(float[] from,float[] to,int faces,
                       float[] pivot,String axis,float angle,float pitch,int yaw){}
    private static final Map<String,List<Part>> CACHE=new HashMap<>();
    private static Object lastResources;
    private IndustrialPaintModelGeometry(){}

    public static List<Part> elements(BlockState state){
        Object resources=Minecraft.getInstance().getResourceManager();
        if(resources!=lastResources){
            CACHE.clear();
            lastResources=resources;
        }
        String cacheKey=state.toString();
        return CACHE.computeIfAbsent(cacheKey,unused->readState(state));
    }

    private static JsonObject resource(Identifier name){
        try{
            var opt=Minecraft.getInstance().getResourceManager().getResource(name);
            if(opt.isEmpty())return null;
            try(var reader=new InputStreamReader(opt.get().open(),StandardCharsets.UTF_8)){
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }catch(Exception e){
            return null; // Missing/built-in/incompatible external asset.
        }
    }
    private static String text(JsonObject j,String key,String fallback){
        return j!=null&&j.has(key)&&j.get(key).isJsonPrimitive()
                ?j.get(key).getAsString():fallback;
    }
    private static Identifier location(String raw,String folder){
        String namespace="minecraft",path=raw;
        int delim=raw.indexOf(':');
        if(delim>=0){namespace=raw.substring(0,delim);path=raw.substring(delim+1);}
        if(path.startsWith(folder+"/"))path=path.substring(folder.length()+1);
        return Identifier.fromNamespaceAndPath(namespace,folder+"/"+path+".json");
    }
    private static <T extends Comparable<T>> String propertyValue(
            BlockState state,Property<T> property){
        return property.getName(state.getValue(property));
    }
    private static Map<String,String> properties(BlockState state){
        Map<String,String> props=new HashMap<>();
        for(Property<?> property:state.getProperties())
            props.put(property.getName(),propertyValue(state,property));
        return props;
    }
    private static boolean matchingVariant(String spec,Map<String,String> props){
        if(spec.isBlank())return true;
        for(String pair:spec.split(",")){
            String[] values=pair.split("=",2);
            if(values.length!=2||!values[1].equals(props.get(values[0].trim())))
                return false;
        }
        return true;
    }
    private static boolean matchingWhen(JsonElement predicate,Map<String,String> props){
        if(predicate==null||predicate.isJsonNull())return true;
        if(!predicate.isJsonObject())return false;
        JsonObject object=predicate.getAsJsonObject();
        if(object.has("OR")){
            for(JsonElement child:object.getAsJsonArray("OR"))
                if(matchingWhen(child,props))return true;
            return false;
        }
        if(object.has("AND")){
            for(JsonElement child:object.getAsJsonArray("AND"))
                if(!matchingWhen(child,props))return false;
            return true;
        }
        for(var entry:object.entrySet()){
            String actual=props.get(entry.getKey());
            if(actual==null)return false;
            boolean valid=false;
            for(String allowed:entry.getValue().getAsString().split("\\|"))
                if(allowed.equals(actual)){valid=true;break;}
            if(!valid)return false;
        }
        return true;
    }
    private static void applied(JsonElement apply,List<Part> result){
        if(result.size()>=LIMIT||apply==null)return;
        if(apply.isJsonArray()){
            JsonArray choices=apply.getAsJsonArray();
            if(!choices.isEmpty())applied(choices.get(0),result);
            return;
        }
        JsonObject object=apply.getAsJsonObject();
        String model=text(object,"model","");
        if(model.isEmpty())return;
        int yaw=object.has("y")?object.get("y").getAsInt():0;
        float pitch=object.has("x")?object.get("x").getAsFloat():0f;
        resolveModel(model,result,new HashSet<>(),yaw,pitch);
    }
    private static List<Part> readState(BlockState state){
        List<Part> result=new ArrayList<>();
        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if(id==null)return List.of();
        var blockstate=resource(Identifier.fromNamespaceAndPath(
                id.getNamespace(),"blockstates/"+id.getPath()+".json"));
        if(blockstate==null)return List.of();
        Map<String,String> props=properties(state);
        if(blockstate.has("variants")){
            JsonObject variants=blockstate.getAsJsonObject("variants");
            for(var entry:variants.entrySet())if(matchingVariant(entry.getKey(),props)){
                applied(entry.getValue(),result);
                break;
            }
        }
        if(blockstate.has("multipart")){
            for(JsonElement part:blockstate.getAsJsonArray("multipart")){
                JsonObject json=part.getAsJsonObject();
                if(!json.has("when")||matchingWhen(json.get("when"),props))
                    applied(json.get("apply"),result);
                if(result.size()>=LIMIT)break;
            }
        }
        // Multipart model pieces can reference the SAME cuboid multiple
        // times. Two paints on exactly the same plane z-fight and produce
        // black seams; eliminate duplicate part+transform definitions once,
        // at resource reload, rather than checking on every rendered frame.
        List<Part> unique=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(Part part:result){
            String key=Arrays.toString(part.from())+Arrays.toString(part.to())
                    +part.faces()+Arrays.toString(part.pivot())
                    +part.axis()+part.angle()+part.pitch()+part.yaw();
            if(seen.add(key))unique.add(part);
        }
        return List.copyOf(unique);
    }
    private static float[] floats(JsonArray a,float[] fallback){
        if(a==null||a.size()!=3)return fallback;
        return new float[]{a.get(0).getAsFloat()/16f,
                a.get(1).getAsFloat()/16f,a.get(2).getAsFloat()/16f};
    }
    private static final String[] FACES={"west","east","down","up","north","south"};
    private static void resolveModel(String model,List<Part> out,
            Set<String> seen,int yaw,float pitch){
        if(out.size()>=LIMIT||!seen.add(model))return;
        JsonObject json=resource(location(model,"models"));
        if(json==null)return;
        if(!json.has("elements")){
            String parent=text(json,"parent","");
            if(!parent.isEmpty())resolveModel(parent,out,seen,yaw,pitch);
            return;
        }
        for(JsonElement member:json.getAsJsonArray("elements")){
            if(out.size()>=LIMIT)break;
            JsonObject element=member.getAsJsonObject();
            if(!element.has("from")||!element.has("to"))continue;
            float[] a=floats(element.getAsJsonArray("from"),null);
            float[] b=floats(element.getAsJsonArray("to"),null);
            if(a==null||b==null)continue;
            int faces=0;
            if(element.has("faces")){
                JsonObject faceEntries=element.getAsJsonObject("faces");
                for(int i=0;i<FACES.length;i++)
                    if(faceEntries.has(FACES[i]))faces|=1<<i;
            }
            if(faces==0)continue;
            JsonObject rotation=element.has("rotation")
                    ?element.getAsJsonObject("rotation"):null;
            float[] pivot=floats(rotation==null?null:rotation.getAsJsonArray("origin"),
                    new float[]{.5f,.5f,.5f});
            out.add(new Part(a,b,faces,pivot,
                    text(rotation,"axis",""),rotation==null?0:
                    rotation.has("angle")?rotation.get("angle").getAsFloat():0f,
                    pitch,yaw));
        }
    }
}
