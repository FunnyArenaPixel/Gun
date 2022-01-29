package cn.cookiestudio.gun.guns;

import cn.nukkit.item.customitem.ItemCustom;

public abstract class ItemMagBase extends ItemCustom {
    public ItemMagBase(int id) {
        super(id);
    }

    public ItemMagBase(int id, Integer meta) {
        super(id, meta);
    }

    public ItemMagBase(int id, Integer meta, int count) {
        super(id, meta, count);
    }

    public ItemMagBase(int id, Integer meta, int count, String name) {
        super(id, meta, count, name);
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    public void doInit() {
    }
}
