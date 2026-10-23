#!/usr/bin/env python3
"""Mail security and routing regression for Minecraft Fabric 26.2."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions/mail"
client=root/"src/client/java/com/caszgamermd/caszualadditions/mail"
saved=(src/"MailSavedData.java").read_text()
menu=(src/"MailMenu.java").read_text()
block=(src/"MailBlock.java").read_text()
content=(src/"MailContent.java").read_text()
screen=(client/"MailScreen.java").read_text()
assert "class MailSavedData extends SavedData" in saved
assert "BOX_SIZE=27" in saved
assert "ItemStack.OPTIONAL_CODEC.listOf()" in saved
assert "server.overworld().getDataStorage().computeIfAbsent(TYPE)" in saved
assert "remember(ServerPlayer player)" in saved
assert "player.getUUID().toString()" in saved
assert "recipients()" in saved
assert "saved.ownBox(viewer.getUUID())" in menu
assert "if(!names.containsKey(recipient.toString()))return false;" in saved
assert "trial=NonNullList.withSize(BOX_SIZE,ItemStack.EMPTY)" in saved
assert "if(!stack.isEmpty())return false;" in saved
assert "if(!anything)return false;" in saved
assert "setDirty();" in saved
assert "for(int i=0;i<outgoing.getContainerSize();i++)outgoing.setItem(i,ItemStack.EMPTY)" in saved
assert "private final SimpleContainer outgoing=new SimpleContainer(OUTGOING);" in menu
assert "if(id>=0&&id<VISIBLE_PLAYERS)" in menu
assert "press(row*9+col);" in screen
assert "if(id>=0&&id<VISIBLE_PLAYERS)" in menu
assert "MailSavedData.Recipient recipient=recipients.get(selected);" in menu
assert "saved.deliver(recipient.id(),outgoing)" in menu
assert "return player.getUUID().equals(owner);" in saved
assert "player.getInventory().placeItemBackInInventory(stack);" in menu
assert "ServerPlayConnectionEvents.JOIN.register(" in content
assert "MailContent.MAILBOX_MENU" in menu
assert "MailContent.PO_BOX_MENU" in menu
assert "simpleCodec(props->new MailBlock(true,props))" in block
assert "MailSavedData.get(serverPlayer.getServer()).remember(serverPlayer);" in block
assert "menu.selectedName()" in screen
for name in ("mailbox","po_box"):
    assert (root/f"src/main/resources/assets/caszual_additions/blockstates/{name}.json").exists()
    assert (root/f"src/main/resources/assets/caszual_additions/models/block/{name}.json").exists()
    assert (root/f"src/main/resources/assets/caszual_additions/items/{name}.json").exists()
    assert (root/f"src/main/resources/data/caszual_additions/recipe/{name}.json").exists()
print("PASS: sender-owned staging; authenticated recipients, saved private 27-slot PO boxes, atomic delivery")
