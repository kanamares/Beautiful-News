package com.lgvalle.beaufitulphotos;

import android.util.Log;
import com.lgvalle.beaufitulphotos.elpais.ElPaisService;
import com.lgvalle.beaufitulphotos.elpais.model.Item;
import com.lgvalle.beaufitulphotos.elpais.model.Rss;
import com.lgvalle.beaufitulphotos.elpais.model.Section;
import com.lgvalle.beaufitulphotos.events.GalleryItemsAvailableEvent;
import com.lgvalle.beaufitulphotos.events.GalleryRequestingMoreElementsEvent;
import com.lgvalle.beaufitulphotos.events.NewsItemChosen;
import com.lgvalle.beaufitulphotos.interfaces.BeautifulPhotosScreen;
import com.lgvalle.beaufitulphotos.utils.BusHelper;
import com.squareup.otto.Subscribe;
import retrofit.RetrofitError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lgvalle on 02/08/14.
 */
public class BeautifulNewsPresenterImpl {
	private static final String TAG = BeautifulNewsPresenterImpl.class.getSimpleName();
	final ElPaisService service;
	private final BeautifulPhotosScreen screen;
	Map<Section, List<Item>> map;

	public BeautifulNewsPresenterImpl(ElPaisService service, BeautifulPhotosScreen screen) {
		this.service = service;
		this.screen = screen;
		this.map = new HashMap<Section, List<Item>>();
	}

	@Subscribe
	public void onGalleryRequestingMoreEvent(GalleryRequestingMoreElementsEvent event) {
		Section section = event.getSection();
		List<Item> items = map.get(section);
		if (items == null || items.isEmpty()) {
			Log.d(TAG, "[BeautifulNewsPresenterImpl - onGalleryRequestingMoreEvent] - (line 37): " + "loading new " + section.getParam());
			load(section);
		} else {
			Log.d(TAG, "[BeautifulNewsPresenterImpl - onGalleryRequestingMoreEvent] - (line 40): " + "loading cached " + section.getParam());
			BusHelper.post(new GalleryItemsAvailableEvent<Item, Section>(items, section));
		}
	}

	/**
	 * When a new Gallery Item is selected, clear previous image views and load the new one
	 */
	@Subscribe
	public void onNewsItemChosen(NewsItemChosen event) {
		if (event != null && event.getItem() != null) {
			List<Item> section = map.get(event.getSection());
			int index = section.indexOf(event.getItem());
			screen.openDetails(index, section);
		}
	}

	private void load(final Section section) {
		service.getPortada(section.getParam(), new retrofit.Callback<Rss>() {
			@Override
			public void success(Rss rss, retrofit.client.Response response) {
				if (rss == null) {
					Log.d(TAG, "[BeautifulPhotosPresenterImpl - success] - (line 119): " + "rss null");
				} else if (rss.getChannel() == null) {
					Log.d(TAG, "[BeautifulPhotosPresenterImpl - success] - (line 121): " + "channel null");
				}
				List<Item> items = rss.getChannel().getItem();
				BusHelper.post(new GalleryItemsAvailableEvent<Item, Section>(items, section));
				map.put(section, items);
			}

			@Override
			public void failure(RetrofitError retrofitError) {
				Log.e(TAG, "[BeautifulPhotosPresenterImpl - failure] - (line 124): " + "", retrofitError);
			}
		});
	}
}