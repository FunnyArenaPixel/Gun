package cn.cookiestudio.gun.command;

import cn.cookiestudio.gun.GunPlugin;
import cn.cookiestudio.gun.playersetting.PlayerSettingMap;
import cn.lanink.gamecore.form.element.ResponseElementButton;
import cn.lanink.gamecore.form.windows.AdvancedFormWindowCustom;
import cn.lanink.gamecore.form.windows.AdvancedFormWindowSimple;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.form.element.*;

import java.util.ArrayList;
import java.util.List;

public class GunCommand extends Command {
    public GunCommand(String name) {
        super(name);
        this.setPermission("op");
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        if (commandSender instanceof ConsoleCommandSender){
            commandSender.sendMessage("此命令无法在控制台使用！");
            return true;
        }
        if (strings.length == 0){
            return true;
        }
        Player player = (Player)commandSender;
        if ("data".equals(strings[0])) {
            if (!commandSender.isOp()){
                commandSender.sendMessage("你没有足够的权限使用此命令！");
                return true;
            }
            AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("选择你需要修改参数的枪械:");
            GunPlugin.getInstance().getGunDataMap().values().forEach(gunData -> {
                simple.addButton(new ResponseElementButton(gunData.getGunName(), new ElementButtonImageData("path", "textures/items/book_writable"))
                        .onClicked(cp -> {
                            String gunName = gunData.getGunName();
                            AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(gunName);
                            custom.addElement(new ElementInput("弹夹容量", "magSize", String.valueOf(gunData.getMagSize())));//0
                            custom.addElement(new ElementInput("开火冷却", "fireCoolDown", String.valueOf(gunData.getFireCoolDown())));//1
                            custom.addElement(new ElementInput("换弹时间", "reloadTime", String.valueOf(gunData.getReloadTime())));//2
                            custom.addElement(new ElementInput("站立时缓慢等级", "slownessLevel", String.valueOf(gunData.getSlownessLevel())));//3
                            custom.addElement(new ElementInput("潜行时缓慢等级", "slownessLevelAim", String.valueOf(gunData.getSlownessLevelAim())));//4
                            custom.addElement(new ElementInput("开火视角摇晃程度", "fireSwingIntensity", String.valueOf(gunData.getFireSwingIntensity())));//5
                            custom.addElement(new ElementInput("伤害", "hitDamage", String.valueOf(gunData.getHitDamage())));//6
                            custom.addElement(new ElementInput("范围", "range", String.valueOf(gunData.getRange())));//7
                            custom.addElement(new ElementInput("弹道粒子效果", "particle", gunData.getParticle()));//8
                            custom.addElement(new ElementInput("弹夹名称", "magName", gunData.getMagName()));//9
                            custom.addElement(new ElementInput("后坐力", "recoil", String.valueOf(gunData.getRecoil())));//10
                            custom.addElement(new ElementInput("开火视角摇晃时间", "fireSwingDuration", String.valueOf(gunData.getFireSwingDuration())));//11

                            custom.onResponded((formResponseCustom, cp2) -> {
                                gunData.setMagSize(Integer.parseInt(formResponseCustom.getInputResponse(0)));
                                gunData.setFireCoolDown(Double.parseDouble(formResponseCustom.getInputResponse(1)));
                                gunData.setReloadTime(Double.parseDouble(formResponseCustom.getInputResponse(2)));
                                gunData.setSlownessLevel(Integer.parseInt(formResponseCustom.getInputResponse(3)));
                                gunData.setSlownessLevelAim(Integer.parseInt(formResponseCustom.getInputResponse(4)));
                                gunData.setFireSwingIntensity(Double.parseDouble(formResponseCustom.getInputResponse(5)));
                                gunData.setHitDamage(Double.parseDouble(formResponseCustom.getInputResponse(6)));
                                gunData.setRange(Double.parseDouble(formResponseCustom.getInputResponse(7)));
                                gunData.setParticle(formResponseCustom.getInputResponse(8));
                                gunData.setMagName(formResponseCustom.getInputResponse(9));
                                gunData.setRecoil(Double.parseDouble(formResponseCustom.getInputResponse(10)));
                                gunData.setFireSwingDuration(Double.parseDouble(formResponseCustom.getInputResponse(11)));
                                GunPlugin.getInstance().saveGunData(gunData);
                                cp2.sendMessage("§aSucceed!");
                            });

                            cp.showFormWindow(custom);
                        }));
            });
            player.showFormWindow(simple);
            return true;
        }
        if ("setting".equals(strings[0])){
            AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom("Settings");
            PlayerSettingMap settings = GunPlugin.getInstance().getPlayerSettingPool().getPlayerSettings(player);
            List<String> list = new ArrayList<>();
            list.add(PlayerSettingMap.FireMode.AUTO.name());
            list.add(PlayerSettingMap.FireMode.MANUAL.name());
            custom.addElement(new ElementDropdown("开火模式:",list,settings.getFireMode().ordinal()));
            custom.addElement(new ElementToggle("打开弹道粒子:",settings.isOpenTrajectoryParticle()));
            custom.addElement(new ElementToggle("打开开火烟雾:",settings.isOpenMuzzleParticle()));
            custom.onResponded((formResponseCustom, cp) -> {
                if(formResponseCustom.getDropdownResponse(0).getElementContent().equals(PlayerSettingMap.FireMode.AUTO.name())){
                    settings.setFireMode(PlayerSettingMap.FireMode.AUTO);
                }else{
                    settings.setFireMode(PlayerSettingMap.FireMode.MANUAL);
                }
                settings.setOpenTrajectoryParticle(formResponseCustom.getToggleResponse(1));
                settings.setOpenMuzzleParticle(formResponseCustom.getToggleResponse(2));
                GunPlugin.getInstance().getPlayerSettingPool().write(player.getName(),settings);
                player.sendMessage("§aSucceed!");
            });
            player.showFormWindow(custom);
            return true;
        }
        return true;
    }
}
