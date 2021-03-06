package com.nickimpact.gts.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.nickimpact.gts.GTS;
import com.nickimpact.gts.configuration.MsgConfigKeys;
import com.nickimpact.gts.api.listings.Listing;
import com.nickimpact.gts.entries.items.ItemEntry;
import com.nickimpact.gts.entries.pixelmon.PokemonEntry;
import com.nickimpact.gts.ui.shared.SharedItems;
import com.nickimpact.gts.utils.ItemUtils;
import com.nickimpact.impactor.gui.v2.Icon;
import com.nickimpact.impactor.gui.v2.Layout;
import com.nickimpact.impactor.gui.v2.Page;
import com.nickimpact.impactor.gui.v2.PageDisplayable;
import com.pixelmonmod.pixelmon.enums.EnumPokemon;
import com.pixelmonmod.pixelmon.enums.forms.EnumBidoof;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * (Some note will go here)
 *
 * @author NickImpact
 */
public class MainUI implements PageDisplayable, Observer {

	/** The player viewing the UI */
	private Player player;

	/** The current viewing index of listings */
	private Page page;

	/** The condition to search by for the listings */
	private Collection<Predicate<Listing>> searchConditions = Lists.newArrayList();

	/** Whether or not we should show the player's listings or not */
	private boolean justPlayer = false;

	private static final Icon BORDER = Icon.from(ItemStack.builder().from(Icon.BORDER.getDisplay()).add(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "Click to refresh UI")).build());

	public MainUI(Player player) {
		this(player, Lists.newArrayList());
	}

	/**
	 *
	 *
	 * @param player The player to open the UI for
	 * @param conditions The search condition to apply to the listings available
	 */
	public MainUI(Player player, Collection<Predicate<Listing>> conditions) {
		if(BORDER.getListeners().isEmpty()) {
			BORDER.addListener(clickable -> apply());
		}
		this.player = player;
		this.searchConditions.addAll(conditions);
		this.searchConditions.add(listing -> !listing.hasExpired());

		this.page = Page.builder()
				.property(InventoryTitle.of(Text.of(TextColors.RED, "GTS ", TextColors.GRAY, "\u00BB ", TextColors.DARK_AQUA, "Listings")))
				.property(InventoryDimension.of(9, 6))
				.previous(Icon.from(ItemStack.builder().itemType(Sponge.getRegistry().getType(ItemType.class, "pixelmon:trade_holder_left").orElse(ItemTypes.BARRIER)).build()), 51)
				.current(Icon.from(ItemStack.builder().itemType(ItemTypes.PAPER).build()), 52)
				.next(Icon.from(ItemStack.builder().itemType(Sponge.getRegistry().getType(ItemType.class, "pixelmon:trade_holder_right").orElse(ItemTypes.BARRIER)).build()), 53)
				.layout(this.design())
				.build(GTS.getInstance());
		this.page.define(drawListings(), InventoryDimension.of(7, 3), 1, 1);
//		this.page.getViews().forEach(ui -> {
//			ui.setOpenAction((event, pl) -> {
//				GTS.getInstance().getUpdater().addObserver(this);
//				Sponge.getScheduler().createTaskBuilder()
//						.execute(this::apply)
//						.interval(5, TimeUnit.SECONDS)
//						.name("Main-" + player.getName())
//						.submit(GTS.getInstance());
//			});
//			ui.setCloseAction((event, pl) -> {
//				GTS.getInstance().getUpdater().deleteObserver(this);
//				Sponge.getScheduler().getTasksByName("Main-" + player.getName()).forEach(Task::cancel);
//			});
//		});
	}

	@Override
	public Page getDisplay() {
		return this.page;
	}

	private Layout design() {
		Layout.Builder lb = Layout.builder().dimension(9, 6);
		lb.row(BORDER, 0).row(BORDER, 4);
		lb.column(BORDER, 0).column(BORDER, 8);
		lb.slots(BORDER, 47, 50);

		Text skullTitle = GTS.getInstance().getTextParsingUtils().fetchAndParseMsg(this.player, MsgConfigKeys.UI_ITEMS_PLAYER_TITLE, null, null);
		List<Text> skullLore = GTS.getInstance().getTextParsingUtils().fetchAndParseMsgs(this.player, MsgConfigKeys.UI_ITEMS_PLAYER_LORE, null, null);
		ItemStack skull = ItemUtils.createSkull(player.getUniqueId(), skullTitle, skullLore);
		Icon pInfo = new Icon(skull);
		lb.slot(pInfo, 45);

		Text pLTitle = GTS.getInstance().getTextParsingUtils().fetchAndParseMsg(this.player, MsgConfigKeys.UI_ITEMS_PLAYER_LISTINGS_TITLE, null, null);
		List<Text> pLLore = Lists.newArrayList(Text.of(TextColors.GRAY, "Status: ", this.justPlayer ? Text.of(TextColors.GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled")), Text.EMPTY);
		ImmutableList<Text> additional = ImmutableList.copyOf(GTS.getInstance().getTextParsingUtils().fetchAndParseMsgs(this.player, MsgConfigKeys.UI_ITEMS_PLAYER_LISTINGS_LORE, null, null));
		pLLore.addAll(additional);
		ItemStack pListings = ItemStack.builder().itemType(ItemTypes.WRITTEN_BOOK).add(Keys.DISPLAY_NAME, pLTitle).add(Keys.ITEM_LORE, pLLore).build();
		Icon pl = new Icon(pListings);
		pl.addListener(clickable -> {
			this.justPlayer = !this.justPlayer;
			List<Text> lore = Lists.newArrayList(Text.of(TextColors.GRAY, "Status: ", this.justPlayer ? Text.of(TextColors.GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled")));
			lore.addAll(additional);
			pl.getDisplay().offer(Keys.ITEM_LORE, lore);
			this.page.apply(pl, 46);
			this.apply();
		});
		lb.slot(pl, 46);

		Predicate<Listing> pokeCondition = listing -> listing.getEntry() instanceof PokemonEntry;
		Predicate<Listing> itemCondition = listing -> listing.getEntry() instanceof ItemEntry;

		ItemStack pokemon = SharedItems.pokemonDisplay(EnumPokemon.Bidoof, EnumBidoof.SIRDOOFUSIII.getForm(), false, false);
		pokemon.offer(Keys.DISPLAY_NAME, Text.of(
				TextColors.YELLOW, "Show only Pokemon?"
		));
		pokemon.offer(Keys.ITEM_LORE, Lists.newArrayList(
				Text.of(TextColors.GRAY, "Status: ", hasCondition(pokeCondition) ? Text.of(TextColors.GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled"))
		));
		ItemStack items = ItemStack.builder()
				.itemType(ItemTypes.DIAMOND)
				.add(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "Show only Items?"))
				.add(Keys.ITEM_LORE, Lists.newArrayList(
						Text.of(TextColors.GRAY, "Status: ", hasCondition(itemCondition) ? Text.of(TextColors.GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled"))
				))
				.build();

		Icon pIcon = Icon.from(pokemon);
		pIcon.addListener(clickable -> {
			if(this.hasCondition(pokeCondition)) {
				this.searchConditions.remove(pokeCondition);
			} else {
				if(this.hasCondition(itemCondition)) {
					this.searchConditions.remove(itemCondition);
				}

				this.searchConditions.add(pokeCondition);
			}
			pokemon.offer(Keys.ITEM_LORE, Lists.newArrayList(
					Text.of(TextColors.GRAY, "Status: ", hasCondition(pokeCondition) ? Text.of(TextColors.GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled"))
			));
			items.offer(Keys.ITEM_LORE, Lists.newArrayList(
					Text.of(TextColors.GRAY, "Status: ", hasCondition(itemCondition) ? Text.of(TextColors.GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled"))
			));
			this.page.apply(new Icon(pokemon), 48);
			this.apply();
		});
		lb.slot(pIcon, 48);

		Icon iIcon = Icon.from(items);
		iIcon.addListener(clickable -> {
			if(this.hasCondition(itemCondition)) {
				this.searchConditions.remove(itemCondition);
			} else {
				if(this.hasCondition(pokeCondition)) {
					this.searchConditions.remove(pokeCondition);
				}

				this.searchConditions.add(itemCondition);
			}
			pokemon.offer(Keys.ITEM_LORE, Lists.newArrayList(
					Text.of(TextColors.GRAY, "Status: ", hasCondition(pokeCondition) ? Text.of(TextColors.GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled"))
			));
			items.offer(Keys.ITEM_LORE, Lists.newArrayList(
					Text.of(TextColors.GRAY, "Status: ", hasCondition(itemCondition) ? Text.of(TextColors.GREEN, "Enabled") : Text.of(TextColors.RED, "Disabled"))
			));
			this.page.apply(new Icon(items), 49);
			this.apply();
		});
		lb.slot(iIcon, 49);

		return lb.build();
	}

	private boolean hasCondition(Predicate<Listing> predicate) {
		return this.searchConditions.contains(predicate);
	}

	private void apply() {
		List<Icon> icons = this.drawListings();
		this.page.update(icons, InventoryDimension.of(7, 3), 1, 1);
	}

	private List<Listing> getListings() {
		List<Listing> listings;
		if(justPlayer) {
			listings = GTS.getInstance().getListingsCache().stream().filter(listing -> listing.getOwnerUUID().equals(this.player.getUniqueId())).collect(Collectors.toList());
		} else {
			if (!this.searchConditions.isEmpty()) {
				listings = GTS.getInstance().getListingsCache().stream().filter(listing -> {
					boolean passed = false;
					for(Predicate<Listing> predicate : this.searchConditions) {
						passed = predicate.test(listing);
					}

					return passed;
				}).collect(Collectors.toList());
			} else {
				listings = GTS.getInstance().getListingsCache();
			}
		}

		listings = listings.stream().filter(listing -> !listing.hasExpired()).collect(Collectors.toList());
		return listings;
	}

	private List<Icon> drawListings() {
		List<Icon> icons = Lists.newArrayList();
		for(Listing listing : this.getListings()) {
			Icon icon = new Icon(listing.getDisplay(this.player, false));
			icon.addListener(clickable -> {
				UUID uuid = listing.getUuid();
				if(GTS.getInstance().getListingsCache().stream().anyMatch(listing1 -> listing1.getUuid() == uuid)) {
					Sponge.getScheduler().createTaskBuilder()
							.execute(() -> {
								this.close(player);
								new ConfirmUI(this.player, listing, searchConditions).open(player);
							})
							.delayTicks(1)
							.submit(GTS.getInstance());
				}
			});
			icons.add(icon);
		}

		return icons;
	}

	@Override
	public void update(Observable o, Object arg) {
		this.apply();
	}
}
