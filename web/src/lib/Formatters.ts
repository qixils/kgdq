const date_header_format = new Intl.DateTimeFormat(undefined, { dateStyle: 'full' });
// When translated: use translation locale
const date_hero_format = new Intl.DateTimeFormat("en-US", { dateStyle: 'long' });
const time_format = new Intl.DateTimeFormat(undefined, { timeStyle: 'short' });

export class Formatters {
    private money_format: Intl.NumberFormat;

    constructor(money_currency: string) {
        this.money_format = new Intl.NumberFormat(undefined, { style: 'currency', currency: money_currency });
    }

    static date_header(dt: string) {
        return date_header_format.format(new Date(dt));
    }

    static date_hero(dt: string) {
        return date_hero_format.format(new Date(dt));
    }

    static date_weekday_date(dt: string) {
        return new Intl.DateTimeFormat(undefined,
            { weekday: "long", month: "long", day: "numeric", }
        ).format(new Date(dt));
    }

    static time(dt: string) {
        return time_format.format(new Date(dt));
    }

    money(amount: number) {
        return this.money_format.format(amount);
    }
}