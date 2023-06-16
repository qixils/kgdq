// TODO When translated: use translation locale
const date_header_format = new Intl.DateTimeFormat("en-US", { dateStyle: 'full' });
const date_hero_format = new Intl.DateTimeFormat("en-US", { dateStyle: 'long' });
const time_format = new Intl.DateTimeFormat(undefined, { timeStyle: 'short' });
const date_weekday_date_format = new Intl.DateTimeFormat("en-US", { weekday: "long", month: "long", day: "numeric" });

export class Formatters {
    private money_format: Intl.NumberFormat;

    constructor(money_currency: string) {
        this.money_format = new Intl.NumberFormat(undefined, { style: 'currency', currency: money_currency,
            maximumFractionDigits: 0,
            minimumFractionDigits: 0,  });
    }

    static date_header(dt: string) {
        return date_header_format.format(new Date(dt));
    }

    static date_hero(dt: string) {
        return date_hero_format.format(new Date(dt));
    }

    static date_weekday_date(dt: string) {
        return date_weekday_date_format.format(new Date(dt));
    }

    static time(dt: string) {
        return time_format.format(new Date(dt));
    }

    money(amount: number) {
        return this.money_format.format(amount);
    }
}