package me.ellieis.Sabotage.game.shop.items;

import java.util.List;

public class ShopItems {
    public static PlayerTracker PLAYER_TRACKER = new PlayerTracker();
    public static TesterBypass TESTER_BYPASS = new TesterBypass();
    public static TesterRecharge TESTER_RECHARGE = new TesterRecharge();
    public static TrappedChest TRAPPED_CHEST = new TrappedChest();
    public static WoodSpear WOODEN_SPEAR = new WoodSpear();
    public static List<BaseShopItem> ITEMS = List.of(ShopItems.TESTER_RECHARGE, ShopItems.PLAYER_TRACKER, ShopItems.TESTER_BYPASS, ShopItems.TRAPPED_CHEST, ShopItems.WOODEN_SPEAR);

    public static void register() {}
}
