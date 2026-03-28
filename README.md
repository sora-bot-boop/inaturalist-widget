# Nature Widget 🌿

An Android widget that displays beautiful nature photos from [iNaturalist](https://www.inaturalist.org/), the citizen science platform for sharing biodiversity observations.

## Features

- 🖼️ **Home screen widget** displaying nature photos
- 🔄 **Auto-refresh** every 4 hours with new images
- 👆 **Tap to refresh** for instant new content
- 🏷️ **Species info** overlay showing common name, scientific name, and location
- 📱 **Resizable widget** - works in various sizes
- 🌙 **Dark mode** support

## Screenshots

*Coming soon*

## Installation

### From Source

1. Clone this repository
2. Open in Android Studio (Arctic Fox or newer)
3. Build and run on your device

```bash
git clone https://github.com/sora-bot-boop/inaturalist-widget.git
cd inaturalist-widget
./gradlew assembleDebug
```

### Requirements

- Android 8.0 (API 26) or higher
- Internet connection for fetching images

## Usage

1. **Install the app** on your Android device
2. **Add the widget** to your home screen:
   - Long press on home screen
   - Select "Widgets"
   - Find "Nature Widget" and drag it to your home screen
3. **Tap the widget** to load a new nature photo
4. Photos update automatically every 4 hours

## iNaturalist API

This app uses the [iNaturalist API v1](https://api.inaturalist.org/v1/docs/) to fetch research-grade observations with photos.

### Key Endpoints Used

```
GET https://api.inaturalist.org/v1/observations
```

### Query Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `quality_grade` | Filter by verification status | `research` |
| `photos` | Only observations with photos | `true` |
| `per_page` | Results per page (max 200) | `20` |
| `order_by` | Sort order | `random` |
| `taxon_id` | Filter by taxon ID | `3` (birds) |
| `iconic_taxa` | Filter by iconic taxon | `Aves`, `Mammalia` |
| `lat`, `lng`, `radius` | Location-based filter | `48.8566`, `2.3522`, `50` |

### Photo URLs

Photos come with a `square` size by default. Replace in URL to get other sizes:
- `/square.jpg` → 75x75
- `/small.jpg` → 240x240
- `/medium.jpg` → 500px max
- `/large.jpg` → 1024px max
- `/original.jpg` → Full resolution

### Example Response

```json
{
  "total_results": 100000,
  "results": [
    {
      "id": 12345,
      "species_guess": "Red Fox",
      "place_guess": "Paris, France",
      "photos": [
        {
          "url": "https://inaturalist-open-data.s3.amazonaws.com/photos/123/square.jpg",
          "attribution": "(c) John Doe, some rights reserved (CC BY-NC)"
        }
      ],
      "taxon": {
        "name": "Vulpes vulpes",
        "preferred_common_name": "Red Fox",
        "iconic_taxon_name": "Mammalia"
      }
    }
  ]
}
```

## Architecture

```
app/
├── data/
│   ├── api/           # Retrofit API definitions
│   └── repository/    # Data repository
├── widget/
│   ├── NatureWidget.kt          # Glance widget UI
│   ├── NatureWidgetReceiver.kt  # Widget broadcast receiver
│   └── NatureWidgetWorker.kt    # Background update worker
├── ui/theme/          # Compose theme
└── MainActivity.kt    # Main app activity
```

### Technologies

- **Kotlin** - Primary language
- **Jetpack Compose** - UI framework
- **Glance** - Widget framework
- **Retrofit** - HTTP client
- **Kotlinx Serialization** - JSON parsing
- **Coil** - Image loading
- **WorkManager** - Background tasks

## Future Improvements

- [ ] Filter by taxon (birds, mammals, plants, etc.)
- [ ] Location-based filtering (observations near you)
- [ ] Multiple widget sizes with different layouts
- [ ] Favorite/save observations
- [ ] Live wallpaper mode
- [ ] iOS version

## Credits

- Wildlife data and images from [iNaturalist](https://www.inaturalist.org/)
- iNaturalist is a joint initiative of the California Academy of Sciences and the National Geographic Society

## License

MIT License - See [LICENSE](LICENSE) for details.

Images from iNaturalist are subject to their individual licenses (typically Creative Commons).
