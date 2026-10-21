# Crafting Vending Machine (Fabric 26.2)

This is a **craft-on-demand** vending machine, separate from the
Player Head Vending Machine. It does NOT grant free items.

## Basic use
1. Craft and place a Crafting Vending Machine.
2. **Shift-right-click** to open the settings and ingredient cabinet.
3. Place **one sample of each required ingredient** in the 3×3 RECIPE
   area, matching its crafting-table arrangement. These nine slots hold
   real items as reusable recipe samples; they are **not consumed** on vend.
   You can remove/change the samples from the same GUI.
4. Fill the **27-slot STORAGE** grid with the actual ingredients the
   machine must consume. Shift-clicking from player inventory fills this
   cabinet, not the recipe template.
5. The RESULT preview shows the output using the world's current vanilla
   or modded crafting recipes. Adjust the **- / +** buttons to choose
   **1–64 complete crafting operations** per normal click. Right-click a The screen
   quantity button to change by eight operations. The screen shows the
   total number of produced output items (recipe yield ×
   selected operations).
6. **Right-click** the machine normally to craft and dispense the batch
   into the customer's inventory; overflow goes to the world if inventory
   is full. Recipe remnants such as buckets are also returned.

## Safety and limits
- Recipe matching and item withdrawals happen on the **server**.
- Ingredients are drawn only from the 27 stock slots. Recipe samples
  remain stored for reuse and can be removed by the owner.
- An entire requested batch is **simulated before withdrawing anything**.
  A missing ingredient means no items are consumed and nothing is vended.
- Duplicate ingredients in a crafting pattern each consume one item per
  crafting operation. The machine rechecks the recipe on each vend to
  reflect datapack or recipe changes.
- Stock and the template persist with block entity data. Breaking the
  machine in survival drops all 36 stored stacks separately.
- For a recipe producing multiple items, select the number of operations
  based on the total shown beside the preview; e.g. 16 stick crafts
  give 64 sticks.
- No coins or payments required. Additional vending commerce modes are
  not included in this implementation.

## Live checks
Test patterned and shapeless recipes, duplicate inputs, container
remainder recipes, output overflow, world reload, multiple users, and
modded crafting types in Minecraft. The automated CI tests cannot
fully simulate user clicks or container screens.
