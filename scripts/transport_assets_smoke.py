import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
assets=root/'assets/caszual_additions'
models=assets/'models/block/transport'
for p in models.glob('*.json'):
    m=json.loads(p.read_text())
    for element in m['elements']:
        assert all(a<b for a,b in zip(element['from'],element['to'])),p
        assert all(-16<=v<=32 for k in ('from','to') for v in element[k]),p
        for face in element['faces'].values():assert face['texture'][1:] in m['textures'],p
names=['conveyor_straight','conveyor_left','conveyor_right','conveyor_up','conveyor_down','pallet_rail','pallet_rail_left','pallet_rail_right','drone_parking_bay']
recipes=set()
for name in names+['pallet_engine','pallet_car','passenger_car','pallet_drone','transport_controller']:
    item=json.loads((assets/f'items/{name}.json').read_text())
    assert (assets/('models/'+item['model']['model'].split(':')[1]+'.json')).exists(),name
    recipe=json.loads((root/f'data/caszual_additions/recipe/{name}.json').read_text())
    signature=json.dumps([recipe['pattern'],recipe['key']],sort_keys=True)
    assert signature not in recipes,name
    recipes.add(signature)
for name in names:
    states=json.loads((assets/f'blockstates/{name}.json').read_text())['variants']
    assert len(states)==36,name
    for variant in states.values():assert (assets/('models/'+variant['model'].split(':')[1]+'.json')).exists(),name
print('Transport assets: model bounds, 36-state modules, item references and unique recipes passed.')
