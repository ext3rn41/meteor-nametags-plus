package dev.ext3rn41.meteornametagsplus.modules;

import dev.ext3rn41.meteornametagsplus.utils.TextRendererUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Vector3d;

import java.util.*;

import static dev.ext3rn41.meteornametagsplus.utils.ItemUtil.getItem;

public class NametagsPlus extends Module {

    private final Map<UUID, Integer> totemPops = new HashMap<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgRender = settings.createGroup("Render");

    public enum Durability {
        None,
        Total,
        Percentage
    }

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Select entities to draw nametags on.")
            .defaultValue(EntityType.PLAYER, EntityType.ITEM)
            .build()
    );

    private final Setting<Double> minScale = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-scale")
            .description("The minimum scale of the nametag.")
            .defaultValue(1.1)
            .min(0.1)
            .build()
    );

    private final Setting<Double> maxScale = sgGeneral.add(new DoubleSetting.Builder()
            .name("max-scale")
            .description("The maximum scale of the nametag.")
            .defaultValue(1.1)
            .min(0.1)
            .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-self")
            .description("Ignore yourself when in third person or freecam.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignore rendering nametags for friends.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreBots = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-bots")
            .description("Only render non-bot nametags.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
            .name("culling")
            .description("Only render a certain number of nametags at a certain distance.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> maxCullRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("culling-range")
            .description("Only render nametags within this distance of your player.")
            .defaultValue(20)
            .min(0)
            .sliderMax(200)
            .visible(culling::get)
            .build()
    );

    private final Setting<Integer> maxCullCount = sgGeneral.add(new IntSetting.Builder()
            .name("culling-count")
            .description("Only render this many nametags.")
            .defaultValue(50)
            .min(1)
            .sliderRange(1, 100)
            .visible(culling::get)
            .build()
    );


    // 플레이어
    private final Setting<Boolean> displayHealth = sgPlayers.add(new BoolSetting.Builder()
            .name("health")
            .description("Shows the player's health.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> useFloat = sgPlayers.add(new BoolSetting.Builder()
            .name("use-float")
            .description("Use float value.")
            .defaultValue(true)
            .visible(displayHealth::get)
            .build()
    );

    private final Setting<Boolean> displayGameMode = sgPlayers.add(new BoolSetting.Builder()
            .name("gamemode")
            .description("Shows the player's GameMode.")
            .defaultValue(false)
            .build()
    );


    private final Setting<Boolean> displayGmBrackets = sgPlayers.add(new BoolSetting.Builder()
            .name("show-brackets")
            .description("Shows square brackets around the gamemode text.")
            .defaultValue(false)
            .visible(displayGameMode::get)
            .build()
    );

    private final Setting<Boolean> displayDistance = sgPlayers.add(new BoolSetting.Builder()
            .name("distance")
            .description("Shows the distance between you and the player.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> displayPing = sgPlayers.add(new BoolSetting.Builder()
            .name("ping")
            .description("Shows the player's ping.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayPingBrackets = sgPlayers.add(new BoolSetting.Builder()
            .name("show-square-brackets")
            .description("Shows square brackets around the gamemode text.")
            .defaultValue(false)
            .visible(displayPing::get)
            .build()
    );

    private final Setting<Boolean> displayItems = sgPlayers.add(new BoolSetting.Builder()
            .name("items")
            .description("Displays armor and hand items above the name tags.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayTotemPops = sgPlayers.add(new BoolSetting.Builder()
            .name("totem-counter")
            .description("Shows how many totems a player has popped as a negative number.")
            .defaultValue(true)
            .build()
    );

    /*
    추후에 쓸거임
    private final Setting<Integer> maxTotemCount = sgPlayers.add(new IntSetting.Builder()
            .name("max-totem-count")
            .description("Maximum number of totems to count for the color gradient.")
            .defaultValue(37)
            .min(1)
            .max(1000)
            .visible(displayTotemPops::get)
            .build()
    );
     */

    private final Setting<Double> itemSpacing = sgPlayers.add(new DoubleSetting.Builder()
            .name("item-spacing")
            .description("The spacing between items.")
            .defaultValue(2)
            .range(0, 10)
            .visible(displayItems::get)
            .build()
    );

    private final Setting<Boolean> ignoreHandItems = sgPlayers.add(new BoolSetting.Builder()
            .name("ignore-hand-items")
            .description("Doesn't show main-hand / off-hand items that the player has equipped.")
            .defaultValue(false)
            .visible(displayItems::get)
            .build()
    );

    private final Setting<Boolean> ignoreEmpty = sgPlayers.add(new BoolSetting.Builder()
            .name("ignore-empty-slots")
            .description("Doesn't add spacing where an empty item stack would be.")
            .defaultValue(true)
            .visible(displayItems::get)
            .build()
    );

    private final Setting<Durability> itemDurability = sgPlayers.add(new EnumSetting.Builder<Durability>()
            .name("durability")
            .description("Displays item durability as either a total, percentage, or neither.")
            .defaultValue(Durability.None)
            .visible(displayItems::get)
            .build()
    );

    private final Setting<Boolean> displayEnchants = sgPlayers.add(new BoolSetting.Builder()
            .name("display-enchants")
            .description("Displays item enchantments on the items.")
            .defaultValue(false)
            .visible(displayItems::get)
            .build()
    );

    private final Setting<Set<RegistryKey<Enchantment>>> shownEnchantments = sgPlayers.add(new EnchantmentListSetting.Builder()
            .name("shown-enchantments")
            .description("The enchantments that are shown on nametags.")
            .visible(() -> displayItems.get() && displayEnchants.get())
            .defaultValue(
                    Enchantments.PROTECTION,
                    Enchantments.BLAST_PROTECTION,
                    Enchantments.FIRE_PROTECTION,
                    Enchantments.PROJECTILE_PROTECTION
            )
            .build()
    );

    private final Setting<Nametags.Position> enchantPos = sgPlayers.add(new EnumSetting.Builder<Nametags.Position>()
            .name("enchantment-position")
            .description("Where the enchantments are rendered.")
            .defaultValue(Nametags.Position.Above)
            .visible(() -> displayItems.get() && displayEnchants.get())
            .build()
    );

    private final Setting<Integer> enchantLength = sgPlayers.add(new IntSetting.Builder()
            .name("enchant-name-length")
            .description("The length enchantment names are trimmed to.")
            .defaultValue(3)
            .range(1, 5)
            .sliderRange(1, 5)
            .visible(() -> displayItems.get() && displayEnchants.get())
            .build()
    );

    private final Setting<Double> enchantTextScale = sgPlayers.add(new DoubleSetting.Builder()
            .name("enchant-text-scale")
            .description("The scale of the enchantment text.")
            .defaultValue(1)
            .range(0.1, 2)
            .sliderRange(0.1, 2)
            .visible(() -> displayItems.get() && displayEnchants.get())
            .build()
    );

    private final Setting<Boolean> itemCount = sgItems.add(new BoolSetting.Builder()
            .name("show-count")
            .description("Displays the number of items in the stack.")
            .defaultValue(true)
            .build()
    );

    // 렌더
    private final Setting<Boolean> shadow = sgRender.add(new BoolSetting.Builder()
            .name("render-shadow")
            .description("Renders text shadow")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> background = sgRender.add(new ColorSetting.Builder()
            .name("background-color")
            .description("The color of the nametag background.")
            .defaultValue(new SettingColor(0, 0, 0, 75))
            .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
            .name("name-color")
            .description("The color of the nametag names.")
            .defaultValue(new SettingColor())
            .build()
    );

    private final Setting<SettingColor> pingColor = sgRender.add(new ColorSetting.Builder()
            .name("ping-color")
            .description("The color of the nametag ping.")
            .defaultValue(new SettingColor(20, 170, 170))
            .visible(displayPing::get)
            .build()
    );

    private final Setting<SettingColor> gamemodeColor = sgRender.add(new ColorSetting.Builder()
            .name("gamemode-color")
            .description("The color of the nametag gamemode.")
            .defaultValue(new SettingColor(232, 185, 35))
            .visible(displayGameMode::get)
            .build()
    );

    private final Setting<Nametags.DistanceColorMode> distanceColorMode = sgRender.add(new EnumSetting.Builder<Nametags.DistanceColorMode>()
            .name("distance-color-mode")
            .description("The mode to color the nametag distance with.")
            .defaultValue(Nametags.DistanceColorMode.Gradient)
            .visible(displayDistance::get)
            .build()
    );

    private final Setting<SettingColor> distanceColor = sgRender.add(new ColorSetting.Builder()
            .name("distance-color")
            .description("The color of the nametag distance.")
            .defaultValue(new SettingColor(150, 150, 150))
            .visible(() -> displayDistance.get() && distanceColorMode.get() == Nametags.DistanceColorMode.Flat)
            .build()
    );

    private final Setting<SettingColor> totemColor = sgRender.add(new ColorSetting.Builder()
            .name("totem-color")
            .description("The color of the totem pop counter.")
            .defaultValue(new SettingColor(255, 170, 0))
            .build()
    );

    private final Setting<String> nametagOrder = sgRender.add(new StringSetting.Builder()
            .name("nametag-order")
            .description("The nametag text order. Use tokens : %GAMEMODE%, %USERNAME%, %HEALTH%, %PING%, %DISTANCE%, %TOTEM%.")
            .defaultValue("%GAMEMODE% %USERNAME% %HEALTH% %PING% %DISTANCE% %TOTEM%")
            .placeholder("%GAMEMODE% %USERNAME% %HEALTH% %PING% %DISTANCE% %TOTEM%")
            .build()
    );


    private final Color WHITE = new Color(255, 255, 255);
    private final Color RED = new Color(255, 25, 25);
    private final Color REDPINK = new Color(252, 61, 65);
    private final Color AMBER = new Color(255, 188, 72);
    private final Color GREEN = new Color(10, 250, 161);
    private final Color GOLD = new Color(232, 185, 35);

    private final Vector3d pos = new Vector3d();
    private final double[] itemWidths = new double[6];

    private final List<Entity> entityList = new ArrayList<>();


    public NametagsPlus() {
        super(Categories.Render, "nametags+", "Displays customizable nametags above players, items and other entities.");
    }


    private static String ticksToTime(int ticks) {
        if (ticks > 20 * 3600) {
            int h = ticks / 20 / 3600;
            return h + " h";
        } else if (ticks > 20 * 60) {
            int m = ticks / 20 / 60;
            return m + " m";
        } else {
            int s = ticks / 20;
            int ms = (ticks % 20) / 2;
            return s + "." + ms + " s";
        }
    }


    @EventHandler
    private void onTick(TickEvent.Post ev) {
        entityList.clear();

        boolean freecamNotActive = !Modules.get().isActive(Freecam.class);
        boolean notThirdPerson = mc.options.getPerspective().isFirstPerson() || !ignoreSelf.get();

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

        Set<UUID> seenPlayers = new HashSet<>();

        assert mc.world != null;
        for (Entity entity : mc.world.getEntities()) {
            EntityType<?> type = entity.getType();

            if (!entities.get().contains(type)) continue;

            if (type == EntityType.PLAYER) {
                PlayerEntity p = (PlayerEntity) entity;
                seenPlayers.add(p.getUuid());

                // 필터링
                if ((ignoreSelf.get() || (freecamNotActive && notThirdPerson)) && entity == mc.player) continue;
                if (EntityUtils.getGameMode(p) == null && ignoreBots.get()) continue;
                if (Friends.get().isFriend(p) && ignoreFriends.get()) continue;
            }

            if (!culling.get() || PlayerUtils.isWithinCamera(entity, maxCullRange.get())) {
                entityList.add(entity);
            }
        }

        totemPops.keySet().removeIf(uuid -> !seenPlayers.contains(uuid));

        entityList.sort(Comparator.comparing(e -> e.squaredDistanceTo(cameraPos)));
    }


    @EventHandler
    private void onRender2D(Render2DEvent event) {
        final int count    = getRenderCount();
        final boolean shadow = this.shadow.get();
        final float tickDelta = event.tickDelta;

        for (int i = count - 1; i >= 0; i--) {
            Entity entity = entityList.get(i);

            Utils.set(pos, entity, tickDelta);
            pos.add(0, getHeight(entity), 0);

            double scale = getScaleFor(entity);

            if (!NametagUtils.to2D(pos, scale)) continue;

            EntityType<?> type = entity.getType();

            if (type == EntityType.PLAYER) {
                renderNametagPlayer(event, (PlayerEntity) entity, shadow);
            }
            else if (type == EntityType.ITEM) {
                renderNametagItem(((ItemEntity) entity).getStack(), shadow);
            }
            else if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
                renderNametagItem(((ItemFrameEntity) entity).getHeldItemStack(), shadow);
            }
            else if (type == EntityType.TNT) {
                renderTntNametag(ticksToTime(((TntEntity) entity).getFuse()), shadow);
            }
            else if (type == EntityType.TNT_MINECART && ((TntMinecartEntity) entity).isPrimed()) {
                renderTntNametag(ticksToTime(((TntMinecartEntity) entity).getFuseTicks()), shadow);
            }
            else if (entity instanceof LivingEntity living) {
                renderGenericLivingNametag(living, shadow);

            }
            else {
                renderGenericNametag(entity, shadow);
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (mc.world == null) return;

        if (packet.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) return;

        if (player == mc.player) return;

        UUID id = player.getUuid();

        totemPops.merge(id, -1, Integer::sum);
    }


    private void renderNametagPlayer(Render2DEvent event, PlayerEntity player, boolean shadow) {
        TextRenderer text = TextRendererUtil.get();
        NametagUtils.begin(pos, event.drawContext);

        GameMode gm = EntityUtils.getGameMode(player);
        String gmText = "BOT";
        if (gm != null) {
            gmText = switch (gm) {
                case SPECTATOR -> "Sp";
                case SURVIVAL  -> "S";
                case CREATIVE  -> "C";
                case ADVENTURE -> "A";
            };
        }
        if (displayGmBrackets.get()) {
            gmText = "[" + gmText + "]";
        }

        String name;
        Color nameColor = PlayerUtils.getPlayerColor(player, this.nameColor.get());
        if (player == mc.player) name = Objects.requireNonNull(Modules.get().get(NameProtect.class)).getName(player.getName().getString());
        else name = player.getName().getString();

        float absorption = player.getAbsorptionAmount();
        float rawHealth = player.getHealth() + absorption;
        float maxHealth = player.getMaxHealth() + absorption;
        double healthPercentage = rawHealth / Math.max(maxHealth, 0.001f);

        String healthText;
        if (useFloat.get()) {
            healthText = String.format("%.1f", rawHealth);
        } else {
            healthText = Integer.toString(Math.round(rawHealth));
        }

        Color healthColor;
        if (healthPercentage <= 0.333)      healthColor = REDPINK;
        else if (healthPercentage <= 0.666) healthColor = AMBER;
        else                                healthColor = GREEN;

        // 핑
        int ping = EntityUtils.getPing(player);
        String pingText;
        if (displayPingBrackets.get()) pingText = "[" + ping + "ms]";
        else pingText = ping + "ms";

        double dist = Math.round(PlayerUtils.distanceToCamera(player) * 10.0) / 10.0;
        String distText = dist + "m";

        boolean renderPlayerDistance = player != mc.getCameraEntity() || Modules.get().isActive(Freecam.class);

        String order = nametagOrder.get().trim();
        if (order.isEmpty()) {
            order = "%GAMEMODE% %USERNAME% %HEALTH% %PING% %DISTANCE% %TOTEM%";
        }

        List<String> segTexts  = new ArrayList<>();
        List<Color>  segColors = new ArrayList<>();

        String[] tokens = order.split("\\s+");
        for (String token : tokens) {
            switch (token) {
                case "%GAMEMODE%" -> {
                    if (displayGameMode.get()) {
                        segTexts.add(gmText);
                        segColors.add(gamemodeColor.get());
                    }
                }
                case "%USERNAME%" -> {
                    segTexts.add(name);
                    segColors.add(nameColor);
                }
                case "%HEALTH%" -> {
                    if (displayHealth.get()) {
                        segTexts.add(healthText);
                        segColors.add(healthColor);
                    }
                }
                case "%PING%" -> {
                    if (displayPing.get()) {
                        segTexts.add(pingText);
                        segColors.add(pingColor.get());
                    }
                }
                case "%DISTANCE%" -> {
                    if (displayDistance.get() && renderPlayerDistance) {
                        Color distColor = switch (distanceColorMode.get()) {
                            case Flat     -> distanceColor.get();
                            case Gradient -> EntityUtils.getColorFromDistance(player);
                        };
                        segTexts.add(distText);
                        segColors.add(distColor);
                    }
                }
                case "%TOTEM%" -> {
                    if (displayTotemPops.get()) {
                        int pops = totemPops.getOrDefault(player.getUuid(), 0);
                        if (pops != 0) {
                            String totemText = Integer.toString(pops);

                            Color popColor = totemColor.get();

                            segTexts.add(totemText);
                            segColors.add(popColor);
                        }
                    }
                }
                default -> {
                    segTexts.add(token);
                    segColors.add(nameColor);
                }
            }
        }

        if (segTexts.isEmpty()) {
            NametagUtils.end(event.drawContext);
            return;
        }

        double spaceWidth = text.getWidth(" ", shadow);

        double width = 0;
        for (int i = 0; i < segTexts.size(); i++) {
            if (i > 0) width += spaceWidth;
            width += text.getWidth(segTexts.get(i), shadow);
        }

        double widthHalf  = width / 2.0;
        double heightDown = text.getHeight(shadow);

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double textX = -widthHalf;
        double textY = -heightDown;

        for (int i = 0; i < segTexts.size(); i++) {
            if (i > 0) textX += spaceWidth;
            textX = text.render(segTexts.get(i), textX, textY, segColors.get(i), shadow);
        }
        text.end();

        if (displayItems.get()) {
            Arrays.fill(itemWidths, 0);
            boolean hasItems = false;
            int maxEnchantCount = 0;

            int[] slotsToRender = ignoreHandItems.get()
                    ? new int[]{1, 2, 3, 4}
                    : new int[]{0, 1, 2, 3, 4, 5};

            for (int slot : slotsToRender) {
                ItemStack itemStack = getItem(player, slot);

                if (itemWidths[slot] == 0 && (!ignoreEmpty.get() || !itemStack.isEmpty())) {
                    itemWidths[slot] = 32 + itemSpacing.get();
                }

                if (!itemStack.isEmpty()) hasItems = true;

                if (displayEnchants.get()) {
                    ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(itemStack);

                    int size = 0;
                    for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
                        if (enchantment.getKey().isPresent()
                                && !shownEnchantments.get().contains(enchantment.getKey().get())) continue;

                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get())
                                + " " + enchantments.getLevel(enchantment);
                        itemWidths[slot] = Math.max(itemWidths[slot], (text.getWidth(enchantName, shadow) / 2));
                        size++;
                    }

                    maxEnchantCount = Math.max(maxEnchantCount, size);
                }
            }

            double itemsHeight = hasItems ? 32 : 0;

            double itemWidthTotal = 0;
            for (int slot : slotsToRender) itemWidthTotal += itemWidths[slot];
            double itemWidthHalf = itemWidthTotal / 2.0;

            double itemY = -heightDown - 7 - itemsHeight;
            double itemX = -itemWidthHalf;

            for (int slot : slotsToRender) {
                ItemStack stack = getItem(player, slot);

                RenderUtils.drawItem(event.drawContext, stack, (int) itemX, (int) itemY, 2, true, null, false);

                // 내구도
                if (stack.isDamageable() && itemDurability.get() != Durability.None) {
                    text.begin(0.75, false, true);

                    String damageText = switch (itemDurability.get()) {
                        case Percentage -> String.format("%.0f%%",
                                ((stack.getMaxDamage() - stack.getDamage()) * 100f) / (float) stack.getMaxDamage());
                        case Total -> Integer.toString(stack.getMaxDamage() - stack.getDamage());
                        default -> "err";
                    };
                    Color damageColor = new Color(stack.getItemBarColor());

                    text.render(damageText, (int) itemX, (int) itemY, damageColor.a(255), true);
                    text.end();
                }

                // 인챈트
                if (maxEnchantCount > 0 && displayEnchants.get()) {
                    text.begin(0.5 * enchantTextScale.get(), false, true);

                    ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
                    Object2IntMap<RegistryEntry<Enchantment>> enchantmentsToShow = new Object2IntOpenHashMap<>();

                    for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
                        if (enchantment.matches(shownEnchantments.get()::contains)) {
                            enchantmentsToShow.put(enchantment, enchantments.getLevel(enchantment));
                        }
                    }

                    double aW = itemWidths[slot];
                    double enchantY = 0;

                    double addY = switch (enchantPos.get()) {
                        case Above -> -((enchantmentsToShow.size() + 1) * text.getHeight(shadow));
                        case OnTop -> (itemsHeight - enchantmentsToShow.size() * text.getHeight(shadow)) / 2;
                    };

                    for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry
                            : Object2IntMaps.fastIterable(enchantmentsToShow)) {

                        String enchantName = Utils.getEnchantSimpleName(entry.getKey(), enchantLength.get())
                                + " " + entry.getIntValue();

                        Color enchantColor = WHITE;
                        if (entry.getKey().isIn(EnchantmentTags.CURSE)) enchantColor = RED;

                        double textWidth = text.getWidth(enchantName, shadow);
                        double enchantX = switch (enchantPos.get()) {
                            case Above -> itemX + (aW / 2.0) - (textWidth / 2.0);
                            case OnTop -> itemX + (aW - textWidth) / 2.0;
                        };

                        text.render(enchantName, enchantX, itemY + addY + enchantY, enchantColor, shadow);
                        enchantY += text.getHeight(shadow);
                    }

                    text.end();
                }

                itemX += itemWidths[slot];
            }
        }
        else if (displayEnchants.get()) {
            displayEnchants.set(false);
        }

        NametagUtils.end(event.drawContext);
    }

    private int getRenderCount() {
        int count = culling.get() ? maxCullCount.get() : entityList.size();
        count = MathHelper.clamp(count, 0, entityList.size());

        return count;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(getRenderCount());
    }

    private double getHeight(Entity entity) {
        double height = entity.getEyeHeight(entity.getPose());

        if (entity.getType() == EntityType.ITEM || entity.getType() == EntityType.ITEM_FRAME || entity.getType() == EntityType.GLOW_ITEM_FRAME) height += 0.2;
        else height += 0.5;

        return height;
    }

    private double getScaleFor(Entity entity) {
        double dist = PlayerUtils.distanceToCamera(entity);

        double minDist = 0.9;
        double maxDist = 40.0;

        double t = (dist - minDist) / (maxDist - minDist);
        t = MathHelper.clamp(t, 0.0, 1.0);

        return MathHelper.lerp(t, maxScale.get(), minScale.get());
    }


    private void drawBg(double x, double y, double width, double height) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x, y, width, height, background.get());
        Renderer2D.COLOR.render();
    }

    private void renderNametagItem(ItemStack stack, boolean shadow) {
        if (stack.isEmpty()) return;

        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String name = Names.get(stack);
        String count = " x" + stack.getCount();

        double nameWidth = text.getWidth(name, shadow);
        double countWidth = text.getWidth(count, shadow);
        double heightDown = text.getHeight(shadow);

        double width = nameWidth;
        if (itemCount.get()) width += countWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(name, hX, hY, nameColor.get(), shadow);
        if (itemCount.get()) text.render(count, hX, hY, GOLD, shadow);
        text.end();

        NametagUtils.end();
    }

    private void renderGenericLivingNametag(LivingEntity entity, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String nameText = entity.getType().getName().getString();
        nameText += " ";

        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercentage = health / (entity.getMaxHealth() + absorption);

        String healthText = String.valueOf(health);
        Color healthColor;

        if (healthPercentage <= 0.333) healthColor = RED;
        else if (healthPercentage <= 0.666) healthColor = AMBER;
        else healthColor = GREEN;

        double nameWidth = text.getWidth(nameText, shadow);
        double healthWidth = text.getWidth(healthText, shadow);
        double heightDown = text.getHeight(shadow);

        double width = nameWidth + healthWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(nameText, hX, hY, nameColor.get(), shadow);
        text.render(healthText, hX, hY, healthColor, shadow);
        text.end();

        NametagUtils.end();
    }

    private void renderGenericNametag(Entity entity, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String nameText = entity.getType().getName().getString();

        double nameWidth = text.getWidth(nameText, shadow);
        double heightDown = text.getHeight(shadow);
        double widthHalf = nameWidth / 2;

        drawBg(-widthHalf, -heightDown, nameWidth, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        text.render(nameText, hX, hY, nameColor.get(), shadow);
        text.end();

        NametagUtils.end();
    }

    private void renderTntNametag(String fuseText, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        double width = text.getWidth(fuseText, shadow);
        double heightDown = text.getHeight(shadow);

        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        text.render(fuseText, hX, hY, nameColor.get(), shadow);
        text.end();

        NametagUtils.end();
    }
}
