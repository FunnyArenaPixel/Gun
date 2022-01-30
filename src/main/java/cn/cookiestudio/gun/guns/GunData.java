package cn.cookiestudio.gun.guns;

import cn.cookiestudio.customparticle.CustomParticlePlugin;
import cn.cookiestudio.customparticle.customparticle.CustomParticle;
import cn.cookiestudio.customparticle.math.BVector3;
import cn.cookiestudio.customparticle.math.MathUtil;
import cn.cookiestudio.customparticle.util.Identifier;
import cn.cookiestudio.gun.GunPlugin;
import cn.cookiestudio.gun.guns.achieve.ItemGunM3;
import cn.cookiestudio.gun.network.AnimateEntityPacket;
import cn.cookiestudio.gun.network.CameraShakePacket;
import cn.cookiestudio.gun.utils.SoundUtil;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.potion.Effect;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Getter
@Setter
public class GunData {

    private static Random random = new Random(System.currentTimeMillis());
    private static String emptyGunSound = "empty_gun";

    private int magSize;
    private double fireCoolDown;
    private double reloadTime;
    private int slownessLevel;
    private int slownessLevelAim;
    private double hitDamage;
    private double range;
    private double recoil;
    private String particle;
    private FireParticle fireParticle;
    private String gunName;
    private String magName;
    private String fireSound;
    private String magInSound;
    private String magOutSound;
    private String reloadAnimationTP;
    private String reloadAnimationFP;
    private String animationControllerTP;
    private String animationControllerFP;
    private int gunId;
    private int magId;
    private double fireSwingIntensity;
    private double fireSwingDuration;

    @Builder
    public GunData(int gunId, int magId, String gunName, String magName, int magSize, double fireCoolDown, double reloadTime, int slownessLevel, int slownessLevelAim, double hitDamage, double range, double recoil, String particle, double fireSwingIntensity, double fireSwingDuration) {
        //storage
        this.gunName = gunName;
        this.magSize = magSize;
        this.fireCoolDown = fireCoolDown;
        this.reloadTime = reloadTime;
        this.slownessLevel = slownessLevel;
        this.slownessLevelAim = slownessLevelAim;
        this.fireSwingIntensity = fireSwingIntensity;
        this.fireSwingDuration = fireSwingDuration;
        this.hitDamage = hitDamage;
        this.range = range;
        this.particle = particle;
        this.magName = magName;
        this.recoil = recoil;

        //dynamic
        this.gunId = gunId;
        this.magId = magId;
        this.fireParticle = new FireParticle(Identifier.from("gun", gunName));
        this.fireSound = gunName + "_fire";
        this.magInSound = gunName + "_magin";
        this.magOutSound = gunName + "_magout";
        this.reloadAnimationFP = "animation."+ this.gunName + ".first_person.reload";
        this.reloadAnimationTP = "animation."+ this.gunName + ".third_person.reload";
        this.animationControllerFP = "controller.animation." + this.gunName + ".first_person";
        this.animationControllerTP = "controller.animation." + this.gunName + ".third_person";
    }

    public void fire(Player player, ItemGunBase gunType) {
        SoundUtil.playSound(player, this.getFireSound(), 1.0F, 1.0F);
        shakeCamera(player);
        if (player.isSprinting()) {
            player.setSprinting(false);
            player.sendMovementSpeed(player.getMovementSpeed());
        }
        Player[] showParticlePlayers = Server
                .getInstance()
                .getOnlinePlayers()
                .values()
                .stream()
                .filter(p -> GunPlugin.getInstance().getPlayerSettingPool().getPlayerSettings(p).isOpenTrajectoryParticle())
                .toArray(Player[]::new);
        if (gunType instanceof ItemGunM3){
            for (int i = 1;i <= 10;i++){
                Location location = player.clone();
                location.yaw += random.nextInt(11) - 5;
                location.pitch += random.nextInt(11) - 5;
                fireParticle.play(location, false,showParticlePlayers);
                //player.setRotation(location.getYaw(),location.getPitch());
            }
        }else {
            fireParticle.play(player, false,showParticlePlayers);
        }
        if (recoil != 0) {
            Vector3 vector3 = getRecoilPos(player, recoil);
            player.setMotion(vector3);
        }
    }

    public void fire(EntityHuman human, ItemGunBase gunType) {
        SoundUtil.playSound(human, this.getFireSound(), 1.0F, 1.0F);
        if (human.isSprinting()) {
            human.setSprinting(false);
        }
        Player[] showParticlePlayers = Server
                .getInstance()
                .getOnlinePlayers()
                .values()
                .stream()
                .filter(p -> GunPlugin.getInstance().getPlayerSettingPool().getPlayerSettings(p).isOpenTrajectoryParticle())
                .toArray(Player[]::new);
        if (gunType instanceof ItemGunM3){
            for (int i = 1;i <= 10;i++){
                Location location = human.clone();
                location.yaw += random.nextInt(11) - 5;
                location.pitch += random.nextInt(11) - 5;
                fireParticle.play(location, false,showParticlePlayers);
            }
        }else {
            fireParticle.play(human, false,showParticlePlayers);
        }
        if (recoil != 0) {
            Vector3 vector3 = getRecoilPos(human, recoil);
            human.setMotion(vector3);
        }
    }

    public void startReload(EntityHuman human) {
        //playReloadAnimation(human);
        SoundUtil.playSound(human, magOutSound, 1.0F, 1.0F);
    }

    public void reloadFinish(EntityHuman human) {
        SoundUtil.playSound(human, magInSound, 1.0F, 1.0F);
    }

    public void emptyGun(EntityHuman human){
        SoundUtil.playSound(human, emptyGunSound, 1.0F, 1.0F);
    }

    public Vector3 getRecoilPos(EntityHuman human, double length) {
        Vector3 pos = MathUtil.getFaceDirection(human, length).addAngle(180, 0).getPos();
        pos.y = human.y;
        return pos;
    }

    public void shakeCamera(Player player){
        CameraShakePacket packet = new CameraShakePacket();
        packet.setDuration(0.1F);
        packet.setShakeType(CameraShakePacket.CameraShakeType.ROTATIONAL);
        packet.setShakeAction(CameraShakePacket.CameraShakeAction.ADD);
        packet.setIntensity((float) fireSwingIntensity);
        player.dataPacket(packet);
    }

    public void playReloadAnimation(Player player){
        AnimateEntityPacket packetTP = new AnimateEntityPacket();
        packetTP.setAnimation(reloadAnimationTP);
        packetTP.setNextState("");
        packetTP.setStopExpression("");
        packetTP.setController(animationControllerTP);
        packetTP.setBlendOutTime(0);
        packetTP.getEntityRuntimeIds().add(player.getId());
        player.dataPacket(packetTP);
        AnimateEntityPacket packetFP = new AnimateEntityPacket();
        packetFP.setAnimation(reloadAnimationFP);
        packetFP.setNextState("");
        packetFP.setStopExpression("");
        packetFP.setController(animationControllerFP);
        packetFP.setBlendOutTime(0);
        packetFP.getEntityRuntimeIds().add(player.getId());
        player.dataPacket(packetFP);
    }

    public void addWalkingSlownessEffect(Player player) {
        Effect effect = Effect.getEffect(Effect.SLOWNESS);
        effect.setAmplifier(this.slownessLevel - 1);
        effect.setVisible(false);
        effect.setDuration(99999);
        player.removeEffect(Effect.SLOWNESS);
        player.addEffect(effect);
    }

    public void addAimingSlownessEffect(Player player) {
        Effect effect = Effect.getEffect(Effect.SLOWNESS);
        effect.setAmplifier(this.slownessLevelAim - 1);
        effect.setVisible(false);
        effect.setDuration(99999);
        player.removeEffect(Effect.SLOWNESS);
        player.addEffect(effect);
    }

    private class FireParticle extends CustomParticle {

        public FireParticle(Identifier identifier) {
            super(identifier);
        }

        @Override
        public Map<String, List<Position>> apply(Long tick, Position pos) {
            if (tick == 2) {
                return null;
            }
            if (!(pos instanceof EntityHuman)){
                return null;
            }
            EntityHuman human = (EntityHuman) pos;
            Location pos1;
            if (human.isSneaking()){
                pos1 = human.getLocation().add(0,-0.15,0);
            }else{
                pos1 = human;
            }
            Map<String, List<Position>> map = new ConcurrentHashMap<>();
            Map<Integer, Position> ammoMap = new ConcurrentHashMap<>();
            Map<Entity, Integer> hitMap = new ConcurrentHashMap<>();
            List<Position> ammoParticleList = new CopyOnWriteArrayList<>();
            List<Position> hitParticleList = new CopyOnWriteArrayList<>();
            BVector3 face = MathUtil.getFaceDirection(pos1, 0.8);
            for (int i = 0; i <= range * 20; i++) {
                Position ammoPos = face.extend(0.05).addToPosition(pos1).add(0, 1.62, 0);
                if (!ammoPos.getLevelBlock().canPassThrough()) break;
                ammoMap.put(i, ammoPos);
                if (i % 4 == 0) ammoParticleList.add(ammoPos);
            }
            ammoMap.entrySet().forEach(entry -> {
                FullChunk chunk = entry.getValue().getChunk();
                if (chunk == null)
                    return;
                chunk.getEntities().values().forEach(entity -> {
                    if (entity.getBoundingBox().isVectorInside(entry.getValue()) && !entity.equals(human)) {
                        if (hitMap.containsKey(entity)) {
                            if (hitMap.get(entity) > entry.getKey()) {
                                hitMap.put(entity, entry.getKey());
                            }
                        } else {
                            hitMap.put(entity, entry.getKey());
                        }
                    }
                });
            });
            hitMap.keySet().forEach(entity -> {
                EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(human,entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK,(float)hitDamage,0F);
                event.setAttackCooldown(0);
                entity.attack(event);
                hitParticleList.add(ammoMap.get(hitMap.get(entity)));
            });
            for (Position hitPos : hitParticleList) {
                hitPos.getLevel().addParticle(new DestroyBlockParticle(hitPos, Block.get(152)));
            }
            map.put(particle, ammoParticleList);
            Position fireSmokePos = MathUtil.getFaceDirection(pos1, 0.8).addToPosition(pos1).add(0, 1.62, 0);
            if (human instanceof Player) {
                if (GunPlugin.getInstance().getPlayerSettingPool().getPlayerSettings((Player) human).isOpenMuzzleParticle()) {
                    CustomParticlePlugin.getInstance().getParticleSender().sendParticle("minecraft:eyeofender_death_explode_particle", fireSmokePos);
                }
            }
            return map;
        }
    }
}
