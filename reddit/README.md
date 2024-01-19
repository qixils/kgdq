# VODThread

This script runs the Reddit [/u/VODThread](https://reddit.com/u/VODThread) for GDQ and ESA.
The VODThread account is currently maintained by [me](https://reddit.com/u/noellekiq), though was previously maintained
by [suudo](https://reddit.com/u/suudo) using [blha303/gdq-scripts](https://github.com/blha303/gdq-scripts/).

The script primarily fetches data from the [Speedrun VOD Club API](../api-server). It secondarily fetches data from the
old [Reddit VOD lists](https://reddit.com/r/VODThread/wiki/index), as well as some supplemental data from the
[Speedrun.com API](../srcom).

## Usage

The JAR can be compiled using `../gradlew build`.
The built file can be found at `build/libs/reddit-X.X.X-all.jar`.
It can be executed using `java -jar reddit-X.X.X-all.jar`.

The first execution will create the `vodthread.yml` config file in the current directory.
The **Username** and **Password** are self-explanatory.
The **Client ID** and **Client Secret** should correspond to a Reddit "personal use script" app.
You can create one [here](https://old.reddit.com/prefs/apps/).
