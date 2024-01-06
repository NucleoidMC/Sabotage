package me.ellieis.Sabotage;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.custom.SabotageBlocks;
import me.ellieis.Sabotage.game.custom.SabotageItems;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import me.ellieis.Sabotage.game.phase.SabotageWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nucleoid.plasmid.game.GameType;

import java.util.ArrayList;
import java.util.List;

public class Sabotage implements ModInitializer {
	public static final String MOD_ID = "sabotage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier SABOTAGE_ID = new Identifier(MOD_ID, "sabotage");
	public static final GameType GAME_TYPE = GameType.register(SABOTAGE_ID, SabotageConfig.CODEC, SabotageWaiting::Open);
	public static final List<SabotageActive> activeGames = new ArrayList<>();

	@Override
	public void onInitialize() {
		SabotageBlocks.register();
		SabotageItems.register();
	}

	public static Identifier identifier(String value) {
		return new Identifier(MOD_ID, value);
	}
}