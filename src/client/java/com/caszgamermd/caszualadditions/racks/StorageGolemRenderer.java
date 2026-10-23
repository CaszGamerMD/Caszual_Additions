package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletData;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.pallet.PalletKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class StorageGolemRenderer
        extends MobRenderer<StorageGolem,StorageGolemRenderState,StorageGolemModel> {
    public static final ModelLayerLocation LAYER=
            new ModelLayerLocation(CaszualAdditions.id("storage_golem"),"main");
    private static final Identifier TEXTURE=CaszualAdditions.id("textures/entity/storage_golem.png");
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    private final ItemModelResolver items;

    public StorageGolemRenderer(EntityRendererProvider.Context ctx){
        super(ctx,new StorageGolemModel(ctx.bakeLayer(LAYER)),.35f);
        blocks=ctx.getBlockModelResolver();
        items=ctx.getItemModelResolver();
        // Layers submit inside LivingEntityRenderer's body-yaw transform;
        // unlike rendering after super.submit(), this stays in the hands
        // when the worker turns, walks, or rotates at a warehouse shelf.
        addLayer(new HeldCargoLayer(this));
    }
    @Override public void extractRenderState(StorageGolem golem,
            StorageGolemRenderState state,float partialTick){
        super.extractRenderState(golem,state,partialTick);
        // Animation is governed by real cargo/missions, never by the
        // custom name. A crew member whose task label still says PALLET
        // must not stand idle with both arms permanently raised.
        state.ridingForklift=golem.getVehicle() instanceof WarehouseForklift;
        if(golem.getVehicle() instanceof WarehouseForklift vehicle){
            // Rotate the driver with the CAB, not golem look/idle animation.
            // The forklift itself supplies the physical seat position and
            // vanilla interpolation of the passenger's world location.
            state.bodyRot=vehicle.getYRot();
            state.yRot=0f;
            state.xRot=0f;
            state.walkAnimationSpeed=0f;
        }
        state.carryingPallet=golem.carryingPalletPose();
        state.pushingEmptyPallet=golem.pushingPalletPose();
        state.showSharedPallet=state.carryingPallet
                &&golem.crewIndex()==0
                &&!golem.displayPallet().isEmpty();
        state.visibleCargo=state.showSharedPallet?golem.visibleCargoCount():0;
        state.heldSlots=state.carryingPallet||state.pushingEmptyPallet
                ?0:golem.displayedLoadSlots();
        // Use the real synced ItemStack and its item model (including
        // modded non-block items). Only the first non-empty cargo sample
        // is displayed between the hands, never a fake 3x3 tray.
        ItemStack held=ItemStack.EMPTY;
        for(int i=0;i<state.heldSlots&&i<9;i++){
            ItemStack candidate=golem.displayedLoad(i);
            if(!candidate.isEmpty()){held=candidate;break;}
        }
        if(!held.isEmpty())
            items.updateForLiving(state.heldItem,held,
                    ItemDisplayContext.FIXED,golem);
        else state.heldItem.clear();
        if(state.showSharedPallet||state.pushingEmptyPallet){
            Block material=Blocks.OAK_PLANKS;
            ItemStack carried=golem.displayPallet();
            boolean plastic=false;
            if(carried.getItem() instanceof BlockItem item
                    &&item.getBlock() instanceof PalletBlock pallet){
                material=switch(pallet.kind()){
                    case IRON->Blocks.IRON_BLOCK;
                    case COPPER->PalletContent.copperTextureBlock(pallet.copperAge());
                    case PLASTIC->PalletContent.PALLET_RENDER_PROXY;
                    default->PalletData.wood(carried).getFirst();
                };
            }
            plastic=carried.getItem() instanceof BlockItem bi
                    &&bi.getBlock() instanceof PalletBlock pb
                    &&pb.kind()==PalletKind.PLASTIC;
            blocks.update(state.palletDeck,material.defaultBlockState(),DISPLAY);
            state.palletDeck.tintLayers().clear();
            if(plastic)state.palletDeck.tintLayers().add(PalletData.plasticColor(carried));
            if(state.pushingEmptyPallet){
                for(int i=0;i<4;i++){
                    Block board=material;
                    if(carried.getItem() instanceof BlockItem wood
                            &&wood.getBlock() instanceof PalletBlock pallet
                            &&pallet.kind()==PalletKind.WOOD)
                        board=PalletData.wood(carried).get(i);
                    blocks.update(state.courierBoards[i],board.defaultBlockState(),DISPLAY);
                    state.courierBoards[i].tintLayers().clear();
                    if(plastic)
                        state.courierBoards[i].tintLayers().add(PalletData.plasticColor(carried));
                }
            }
            blocks.update(state.palletSupport,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY);
            blocks.update(state.crate,
                    PalletContent.CARDBOARD_RENDER_PROXY.defaultBlockState(),DISPLAY);
            blocks.update(state.glass,Blocks.GLASS.defaultBlockState(),DISPLAY);
        }
    }
    /** Render one actual two-by-two pallet platform above the raised hands
     * of all four Golems. Index-0 carrier is the only renderer submitting
     * the pallet: cargo remains owned by the lead server-side Golem. */
    @Override public void submit(StorageGolemRenderState state,PoseStack pose,
            SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        if(state.pushingEmptyPallet){
            // The replacement is a FULL-SIZE 2x2 pallet stood on its SIDE:
            // nearly two blocks wide and two blocks tall, not a 1-block
            // miniature floating at a fixed location on the robot's chest.
            // The golem visibly pushes it ahead of its arms. Entity yaw
            // automatically turns both robot and pallet with the path.
            pose.pushPose();
            float walk=state.walkAnimationPos*.72f;
            float motion=Math.min(1f,state.walkAnimationSpeed*2.6f);
            float bob=(float)Math.abs(Math.sin(walk))*.038f*motion;
            float tilt=(float)Math.sin(walk)*2.5f*motion;
            // Surface of upright deck sits just ahead of outstretched hands.
            pose.translate(0,1.01f+bob,-.80f);
            pose.mulPose(Axis.ZP.rotationDegrees(tilt));
            pose.mulPose(Axis.XP.rotationDegrees(-2.0f
                    +(float)Math.cos(walk)*1.8f*motion));
            pose.translate(0,-1.01f,0);
            // Four full-width, physically distinct horizontal deck boards.
            // Each board has its own saved material, including mixed woods.
            for(int board=0;board<4;board++)
                piece(state.courierBoards[board],state,pose,collector,
                        -.94f,.085f+board*.49f,-.04f,1.88f,.255f,.185f);
            // Side runners/feet seen on edge. Reuse the pallet's ACTUAL
            // material rather than rendering unrelated solid iron bars.
            for(float x:new float[]{-.80f,.65f})
                piece(state.courierBoards[0],state,pose,collector,
                        x,.035f,.075f,.15f,1.83f,.185f);
            // Bottom toe and top lip match a real pallet's 2x2 footprint.
            piece(state.courierBoards[1],state,pose,collector,
                    -.94f,.035f,.045f,1.88f,.14f,.24f);
            piece(state.courierBoards[2],state,pose,collector,
                    -.94f,1.83f,.045f,1.88f,.115f,.22f);
            pose.popPose();
        }
        if(!state.showSharedPallet)return;
        pose.pushPose();
        // Crew index 0 is NW of a 1.8 x 1.8 formation. Four corners
        // remain directly beneath this deck as workers trail the player.
        pose.translate(.04f,1.46f,.04f);
        for(float z:new float[]{.08f,1.48f})
            piece(state.palletDeck,state,pose,collector,.02f,0,z,1.76f,.10f,.24f);
        for(float x:new float[]{.10f,.73f,1.36f})
            piece(state.palletDeck,state,pose,collector,x,.13f,.08f,.32f,.12f,1.64f);
        for(float x:new float[]{.10f,1.60f})
            for(float z:new float[]{.10f,1.60f})
                piece(state.palletSupport,state,pose,collector,x,-.12f,z,.10f,.17f,.10f);
        // The first 27 visible stacks are represented as 3x3x3 cartons,
        // retaining an accurate filled-slot count without client-side
        // duplication of the 216-slot server inventory.
        for(int i=0;i<state.visibleCargo;i++){
            int col=i%3,row=(i/3)%3,layer=i/9;
            piece(state.crate,state,pose,collector,
                    .14f+col*.54f,.27f+layer*.51f,.14f+row*.54f,
                    .51f,.49f,.51f);
        }
        float wallHeight=Math.max(.64f,
                .29f+(float)Math.ceil(state.visibleCargo/9.0)*.51f);
        // Clear, 1px-ish glass panels contain raised cargo on all sides.
        piece(state.glass,state,pose,collector,.04f,.23f,.035f,1.77f,wallHeight,.025f);
        piece(state.glass,state,pose,collector,.04f,.23f,1.74f,1.77f,wallHeight,.025f);
        piece(state.glass,state,pose,collector,.035f,.23f,.06f,.025f,wallHeight,1.68f);
        piece(state.glass,state,pose,collector,1.78f,.23f,.06f,.025f,wallHeight,1.68f);
        pose.popPose();
    }
    /**
     * Vanilla-style held-item render layer. Minecraft passes a pose stack
     * already rotated with the GOLEM body, not the world camera; the model
     * moves the real item to the articulated wrist location. As the Golem
     * turns the item turns, and empty hands have zero render nodes.
     */
    private static final class HeldCargoLayer
            extends RenderLayer<StorageGolemRenderState,StorageGolemModel> {
        HeldCargoLayer(StorageGolemRenderer owner){super(owner);}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,
                int light,StorageGolemRenderState state,float yRot,float xRot){
            if(state.heldSlots<=0||state.carryingPallet
                    ||state.pushingEmptyPallet||state.heldItem.isEmpty())return;
            pose.pushPose();
            getParentModel().translateToCargoHand(pose);
            // The wrist points forwards. These standard hand transforms
            // orient both flat items and cubic blocks naturally.
            pose.mulPose(Axis.XP.rotationDegrees(-90f));
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            pose.translate(0,.015f,-.075f);
            pose.scale(.55f,.55f,.55f);
            state.heldItem.submit(pose,collector,light,
                    OverlayTexture.NO_OVERLAY,state.outlineColor);
            pose.popPose();
        }
    }

    private static void piece(BlockModelRenderState block,StorageGolemRenderState state,
            PoseStack pose,SubmitNodeCollector collector,
            float x,float y,float z,float width,float height,float depth){
        pose.pushPose();
        pose.translate(x,y,z);
        pose.scale(width,height,depth);
        block.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
    }
    @Override public StorageGolemRenderState createRenderState(){
        return new StorageGolemRenderState();
    }
    @Override public Identifier getTextureLocation(StorageGolemRenderState state){
        return TEXTURE;
    }
}
