package cn.cookiestudio.gun.playersetting;

import cn.cookiestudio.gun.GunPlugin;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.utils.Config;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PlayerSettingPool {

    private Config config;
    private final Map<String,PlayerSettingMap> settings = new HashMap<>();

    public PlayerSettingPool(){
        this.init();

        Server.getInstance().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event){
                String name = event.getPlayer().getName();
                if (!settings.containsKey(name)){
                    cache(name);
                }
            }
        }, GunPlugin.getInstance());
        for (Player player : Server.getInstance().getOnlinePlayers().values()){//reload fixed
            cache(player.getName());
        }
    }

    public PlayerSettingMap getPlayerSettings(Player player) {
        return this.getPlayerSettings(player.getName());
    }

    public PlayerSettingMap getPlayerSettings(String playerName) {
        if (!this.settings.containsKey(playerName)) {
            this.cache(playerName);
        }
        return this.settings.get(playerName);
    }

    public void init() {
        this.config = new Config(GunPlugin.getInstance().getDataFolder() + "/playerSettings.json", Config.JSON);
    }

    public PlayerSettingMap cache(String name){
        if (!existInFile(name)){
            PlayerSettingMap entry = PlayerSettingMap
                    .builder()
                    .fireMode(PlayerSettingMap.FireMode.AUTO)
                    .openTrajectoryParticle(false)
                    .openMuzzleParticle(true)
                    .build();
            this.settings.put(name,entry);
            return entry;
        }
        PlayerSettingMap e = PlayerSettingMap
                .builder()
                .fireMode(PlayerSettingMap.FireMode.values()[config.getInt(name + ".fireMode")])
                .openTrajectoryParticle(config.getBoolean(name + ".openTrajectoryParticle"))
                .openMuzzleParticle(config.getBoolean(name + ".openMuzzleParticle"))
                .build();
        this.settings.put(name,e);
        return e;
    }

    public void write(String name,PlayerSettingMap entry){
        this.config.set(name, entry.getMap());
        this.config.save();
    }

    public void writeAll(){
        for (Map.Entry<String, PlayerSettingMap> e : getSettings().entrySet()) {
            write(e.getKey(),e.getValue());
        }
    }

    public boolean existInFile(String name){
        return config.exists(name);
    }

    public void save(){
        this.writeAll();
    }
}
