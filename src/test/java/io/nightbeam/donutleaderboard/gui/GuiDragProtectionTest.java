package io.nightbeam.donutleaderboard.gui;

import io.nightbeam.donutleaderboard.listener.GuiListener;
import io.nightbeam.donutleaderboard.util.InventoryViews;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuiDragProtectionTest {

    @Test
    void dragEventCancelledForPluginGui() {
        GuiManager manager = Mockito.mock(GuiManager.class);
        GuiListener listener = new GuiListener(manager);
        BaseGui gui = Mockito.mock(BaseGui.class);
        InventoryView view = Mockito.mock(InventoryView.class);
        Inventory top = Mockito.mock(Inventory.class);
        when(view.getTopInventory()).thenReturn(top);
        when(top.getHolder()).thenReturn(gui);

        InventoryDragEvent event = Mockito.mock(InventoryDragEvent.class);
        when(event.getView()).thenReturn(view);

        listener.onDrag(event);
        verify(event).setCancelled(true);
        assertTrue(InventoryViews.topHolder(view) instanceof BaseGui);
    }
}
