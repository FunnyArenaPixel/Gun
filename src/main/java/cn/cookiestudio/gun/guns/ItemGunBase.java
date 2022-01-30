package cn.cookiestudio.gun.guns;

import cn.cookiestudio.gun.CoolDownTimer;
import cn.cookiestudio.gun.GunPlugin;
import cn.cookiestudio.gun.playersetting.PlayerSettingMap;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.entity.EntityInteractEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.item.Item;
import cn.nukkit.item.customitem.ItemCustom;
import cn.nukkit.level.GameRule;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AnimatePacket;
import cn.nukkit.potion.Effect;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Map;

@Setter
@Getter
public abstract class ItemGunBase extends ItemCustom {

    protected GunData gunData;

    static {
        Server.getInstance().getPluginManager().registerEvents(new Listener(), GunPlugin.getInstance());
        Server.getInstance().getScheduler().scheduleRepeatingTask(GunPlugin.getInstance(), () -> {
            Server.getInstance().getOnlinePlayers().values().forEach(player -> {
                if (player.getInventory().getItemInHand() instanceof ItemGunBase) {
                    ItemGunBase itemGun = (ItemGunBase) player.getInventory().getItemInHand();
                    if (player.isSneaking()) {
                        itemGun.getGunData().addAimingSlownessEffect(player);
                    }else {
                        itemGun.getGunData().addWalkingSlownessEffect(player);
                    }
                    if (!GunPlugin.getInstance().getCoolDownTimer().isCooling(player) || GunPlugin.getInstance().getCoolDownTimer().getCoolDownMap().get(player).getType() != CoolDownTimer.Type.RELOAD) {
                        if (GunPlugin.getInstance().getPlayerSettingPool().getSettings().containsKey(player.getName()) && GunPlugin.getInstance().getPlayerSettingPool().getPlayerSettings(player).getFireMode() == PlayerSettingMap.FireMode.AUTO) {
                            if (!GunPlugin.getInstance().getFireTask().firing(player)) {
                                player.sendActionBar("<" + itemGun.getAmmoCount() + "/" + itemGun.getGunData().getMagSize() + ">\n§dAUTO MODE: §cOFF");
                            } else {
                                player.sendActionBar("<" + itemGun.getAmmoCount() + "/" + itemGun.getGunData().getMagSize() + ">\n§dAUTO MODE: §aON");
                            }
                        }else{
                            player.sendActionBar("<" + itemGun.getAmmoCount() + "/" + itemGun.getGunData().getMagSize() + ">");
                        }
                        return;
                    }
                    CoolDownTimer.CoolDown coolDown = GunPlugin.getInstance().getCoolDownTimer().getCoolDownMap().get(player);
                    if (coolDown.getType() == CoolDownTimer.Type.RELOAD){
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("RELOAD: §a");
                        int bound = (int)(30.0 * ((double)coolDown.coolDownTick / (itemGun.getGunData().getReloadTime() * 20)));
                        for (int i = 30;i >= 1;i--){
                            if (i < bound) stringBuilder.append("|");
                            if (i == bound) stringBuilder.append("|§c");
                            if (i > bound) stringBuilder.append("|");
                        }
                        player.sendActionBar(stringBuilder.toString(), 0, 1, 0);
                    }
                }
            });
        }, 1);
    }

    public ItemGunBase(int id) {
        super(id);
    }

    public ItemGunBase(int id, Integer meta) {
        super(id, meta);
    }

    public ItemGunBase(int id, Integer meta, int count) {
        super(id, meta, count);
    }

    public ItemGunBase(int id, Integer meta, int count, String name) {
        super(id, meta, count, name);
    }

    public void doInit() {
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public static GunData getGunData(Class<? extends ItemGunBase> clazz) {
        return GunPlugin.getInstance().getGunDataMap().get(clazz);
    }

    public GunInteractAction interact(Player player) {
        if (GunPlugin.getInstance().getCoolDownTimer().isCooling(player)){
            return GunInteractAction.COOLING;
        }
        if (this.getAmmoCount() > 0) {
            gunData.fire(player,this);
            ItemGunBase itemGun = (ItemGunBase)player.getInventory().getItemInHand();
            itemGun.setAmmoCount(itemGun.getAmmoCount() - 1);
            player.getInventory().setItem(player.getInventory().getHeldItemIndex(),itemGun,false);
            GunPlugin.getInstance().getCoolDownTimer().addCoolDown(player, (int) (gunData.getFireCoolDown() * 20), () -> {}, () -> CoolDownTimer.Operator.NO_ACTION, CoolDownTimer.Type.FIRECOOLDOWN);
            return GunInteractAction.FIRE;
        }
        if (this.getAmmoCount() == 0 && (player.getInventory().contains(Item.get(gunData.getMagId())) || player.getGamemode() == Player.CREATIVE)) {
            this.reload(player);
            return GunInteractAction.RELOAD;
        }
        if (this.getAmmoCount() == 0){
            gunData.emptyGun(player);
            return GunInteractAction.EMPTY_GUN;
        }
        return null;
    }

    public GunInteractAction interact(EntityHuman human) {
        if (GunPlugin.getInstance().getCoolDownTimer().isCooling(human)){
            return GunInteractAction.COOLING;
        }
        if (this.getAmmoCount() > 0) {
            gunData.fire(human,this);
            ItemGunBase itemGun = (ItemGunBase)human.getInventory().getItemInHand();
            itemGun.setAmmoCount(itemGun.getAmmoCount() - 1);
            human.getInventory().setItem(human.getInventory().getHeldItemIndex(),itemGun,false);
            GunPlugin.getInstance().getCoolDownTimer().addCoolDown(human, (int) (gunData.getFireCoolDown() * 20), () -> {}, () -> CoolDownTimer.Operator.NO_ACTION, CoolDownTimer.Type.FIRECOOLDOWN);
            return GunInteractAction.FIRE;
        }
        if (this.getAmmoCount() == 0) {
            this.reload(human);
            return GunInteractAction.RELOAD;
        }
        return null;
    }

    public void reload(Player player) {
        gunData.startReload(player);
        GunPlugin.getInstance().getCoolDownTimer().addCoolDown(player, (int) (gunData.getReloadTime() * 20), () -> {
            gunData.reloadFinish(player);
            ItemGunBase itemGun = (ItemGunBase)player.getInventory().getItemInHand();
            itemGun.setAmmoCount(gunData.getMagSize());
            player.getInventory().setItem(player.getInventory().getHeldItemIndex(),itemGun,false); //更新物品
            for (Map.Entry<Integer,Item> entry : player.getInventory().getContents().entrySet()){
                Item item = entry.getValue();
                int slot = entry.getKey();
                if (item.getId() == gunData.getMagId()){
                    item.setCount(item.count - 1);
                    player.getInventory().setItem(slot,item);
                    break;
                }
            }
        }, () -> {
            player.sendMessage("§creload interrupt!");
            return CoolDownTimer.Operator.INTERRUPT;
        }, CoolDownTimer.Type.RELOAD);
    }

    public void reload(EntityHuman human) {
        gunData.startReload(human);
        GunPlugin.getInstance().getCoolDownTimer().addCoolDown(human, (int) (gunData.getReloadTime() * 20), () -> {
            gunData.reloadFinish(human);
            ItemGunBase itemGun = (ItemGunBase)human.getInventory().getItemInHand();
            itemGun.setAmmoCount(gunData.getMagSize());
            human.getInventory().setItem(human.getInventory().getHeldItemIndex(),itemGun,false); //更新物品
            for (Map.Entry<Integer,Item> entry : human.getInventory().getContents().entrySet()){
                Item item = entry.getValue();
                int slot = entry.getKey();
                if (item.getId() == gunData.getMagId()){
                    item.setCount(item.count - 1);
                    human.getInventory().setItem(slot,item);
                    break;
                }
            }
        }, () -> CoolDownTimer.Operator.INTERRUPT, CoolDownTimer.Type.RELOAD);
    }

    public int getAmmoCount(){
        if (this.getNamedTag() != null) {
            return this.getNamedTag().getInt("ammoCount");
        }
        return 0;
    }

    public void setAmmoCount(int count){
        if (this.getNamedTag() != null) {
            this.setNamedTag(this.getNamedTag().putInt("ammoCount", count));
        }else {
            this.setNamedTag(new CompoundTag().putInt("ammoCount", count));
        }
    }

    public abstract ItemMagBase getItemMagObject();

    private static class Listener implements cn.nukkit.event.Listener {
        @EventHandler
        public void onPlayerAnimation(PlayerAnimationEvent event) {
            if (event.getAnimationType() == AnimatePacket.Action.SWING_ARM && event.getPlayer().getInventory().getItemInHand() instanceof ItemGunBase) {
                if (GunPlugin.getInstance().getPlayerSettingPool().getPlayerSettings(event.getPlayer()).getFireMode() == PlayerSettingMap.FireMode.AUTO){
                    GunPlugin.getInstance().getFireTask().changeState(event.getPlayer());
                }else {
                    ((ItemGunBase) event.getPlayer().getInventory().getItemInHand()).interact(event.getPlayer());
                }
            }
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            if (player.getInventory().getItemInHandFast() instanceof ItemGunBase) {
                if (event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
                    ((ItemGunBase) player.getInventory().getItemInHandFast()).reload(player);
                }
            }
        }

        @EventHandler
        public void onEntityInteract(EntityInteractEvent event) {
            if (event.getEntity() instanceof EntityHuman) {
                EntityHuman human = (EntityHuman) event.getEntity();
                Item itemInHand = human.getInventory().getItemInHandFast();
                Item item = Item.get(itemInHand.getId(), itemInHand.getDamage());
                if (item instanceof ItemGunBase) {
                    human.getInventory().setItem(human.getInventory().getHeldItemIndex(), item, false);
                    ((ItemGunBase) item).interact(human);
                }
            }
        }

        @EventHandler
        public void onPlayerDropItem(PlayerDropItemEvent event){
            if (event.getItem() instanceof ItemGunBase){
                event.setCancelled();
                if (event.getPlayer().getInventory().getItemInHand().getId() != event.getItem().getId())
                    return;
                ItemGunBase itemGun = (ItemGunBase) event.getPlayer().getInventory().getItemInHand();
                event.getPlayer().getInventory().clear(event.getPlayer().getInventory().getHeldItemIndex());
                EntityGun entityGun = new EntityGun(event.getPlayer().getChunk(), EntityGun.getDefaultNBT(event.getPlayer()),itemGun.getGunData(),itemGun);
                entityGun.spawnToAll();
            }
            if (event.getItem() instanceof ItemMagBase){
                event.setCancelled();
                if (event.getPlayer().getInventory().getItemInHand().getId() != event.getItem().getId())
                    return;
                ItemMagBase itemMag = (ItemMagBase) event.getPlayer().getInventory().getItemInHand();
                if (itemMag.getCount() - event.getItem().getCount() > 0){
                    itemMag.setCount(itemMag.getCount() - event.getItem().getCount());
                    event.getPlayer().getInventory().setItemInHand(itemMag);
                }else{
                    event.getPlayer().getInventory().clear(event.getPlayer().getInventory().getHeldItemIndex());
                }
                EntityMag entityMag = new EntityMag(event.getPlayer().getChunk(), EntityGun.getDefaultNBT(event.getPlayer()), (ItemMagBase) event.getItem());
                entityMag.spawnToAll();
            }
        }

        @EventHandler
        public void onPlayerInteractEntityGunOrMag(EntityDamageByEntityEvent event) {
            if (event.getEntity() instanceof EntityGun && event.getDamager() instanceof Player){
                event.setCancelled();
                EntityGun entityGun = (EntityGun) event.getEntity();
                int empty = ((Player) event.getDamager()).getInventory().firstEmpty(null);
                if (empty != -1){
                    ((Player) event.getDamager()).getInventory().setItem(empty,entityGun.getItemGun());
                }
                event.getEntity().close();
            }
            if (event.getEntity() instanceof EntityMag && event.getDamager() instanceof Player){
                event.setCancelled();
                EntityMag entityMag = (EntityMag) event.getEntity();
                ItemMagBase itemMag = entityMag.getItemMag();
                int empty = ((Player) event.getDamager()).getInventory().firstEmpty(null);
                if (empty != -1){
                    ((Player) event.getDamager()).getInventory().setItem(empty,itemMag);
                }
                event.getEntity().close();
            }
        }

        @EventHandler
        public void onEntityGunOrMagHurt(EntityDamageEvent event){
            if (event.getEntity() instanceof EntityGun || event.getEntity() instanceof EntityMag){
                event.setCancelled();
            }
        }

        @EventHandler
        public void onPlayerHeldItem(PlayerItemHeldEvent event){
            if (!(event.getItem() instanceof ItemGunBase)) {
                event.getPlayer().removeEffect(Effect.SLOWNESS);
            }
        }

        @EventHandler
        public void onEntityDead(EntityDeathEvent event){
            if (event.getEntity().getLevel().getGameRules().getBoolean(GameRule.KEEP_INVENTORY))
                return;
            Arrays.stream(event.getDrops()).forEach(item -> {
                if (item instanceof ItemGunBase){
                    ItemGunBase itemGun = (ItemGunBase) item;
                    EntityGun entityGun = new EntityGun(event.getEntity().getChunk(), EntityGun.getDefaultNBT(event.getEntity()),itemGun.getGunData(),itemGun);
                    entityGun.spawnToAll();
                }
                if (item instanceof ItemMagBase){
                    ItemMagBase itemMag = (ItemMagBase) item;
                    EntityMag entityMag = new EntityMag(event.getEntity().getChunk(), EntityGun.getDefaultNBT(event.getEntity()), itemMag);
                    entityMag.spawnToAll();
                }
            });
            event.setDrops(Arrays.stream(event.getDrops()).filter(item -> !(item instanceof ItemGunBase || item instanceof ItemMagBase)).toArray(Item[]::new));
        }
    }

    @Override
    public String getTextureName() {
        return this.gunData.getGunName();
    }

    public enum GunInteractAction{
        FIRE,
        RELOAD,
        COOLING,
        EMPTY_GUN
    }
}