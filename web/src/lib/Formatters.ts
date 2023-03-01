const date_header_format = new Intl.DateTimeFormat(undefined, { dateStyle: 'full' });
const date_hero_format = new Intl.DateTimeFormat(undefined, { dateStyle: 'long' });
const time_format = new Intl.DateTimeFormat(undefined, { timeStyle: 'short' });

export class Formatters {
    static date_header(dt: string) {
        return date_header_format.format(new Date(dt));
    }
    static date_hero(dt: string) {
        return date_hero_format.format(new Date(dt));
    }
    static time(dt: string) {
        return time_format.format(new Date(dt));
    }
}