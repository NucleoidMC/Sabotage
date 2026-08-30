package me.ellieis.Sabotage;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.custom.SabotageBlocks;
import me.ellieis.Sabotage.game.custom.SabotageItems;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import me.ellieis.Sabotage.game.phase.SabotageWaiting;
import me.ellieis.Sabotage.game.shop.ShopMenu;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameTypes;

import java.util.ArrayList;
import java.util.List;

public class Sabotage implements ModInitializer {
	public static final String MOD_ID = "sabotage";
	public static final Identifier SHOP_BUY_PACKET_ID = Sabotage.identifier("shop_buy");

	public static final List<SabotageActive> activeGames = new ArrayList<>();

	@Override
	public void onInitialize() {
		SabotageBlocks.register();
		SabotageItems.register();
		ShopMenu.register();
        GameTypes.register(Sabotage.identifier("sabotage"), SabotageConfig.CODEC, SabotageWaiting::Open);
		PolymerResourcePackUtils.addModAssets(MOD_ID);
	}

	public static Identifier identifier(String value) {
		return Identifier.fromNamespaceAndPath(MOD_ID, value);
	}
}