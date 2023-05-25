/**
 * An organization that hosts speedrunning events.
 */
export interface Organization {

    /**
     * The name of the organization.
     */
    displayName: string;

    /**
     * The short name of the organization.
     */
    shortName: string;

    /**
     * The homepage URL of the organization.
     */
    homepageUrl: string;

    /**
     * Indicates whether this organization supports automatic VOD (Video on Demand) link generation.
     */
    autoVODs: boolean;

    /**
     * The amount of money this organization has raised in total.
     * This property may be missing if the endpoint was fetched with `stats=false`.
     */
    amountRaised?: number;

    /**
     * The number of donations this organization has received in total.
     * This property may be missing if the endpoint was fetched with `stats=false`.
     */
    donationCount?: number;
}

/**
 * The status of a timed object.
 * The object can either be upcoming, in progress, or finished.
 */
export type TimeStatus = 'UPCOMING' | 'IN_PROGRESS' | 'FINISHED';

/**
 * An event being hosted by a speedrunning organization.
 */
export interface Event {

    /**
     * The numerical ID of the event.
     */
    id: number;

    /**
     * The event slug or short name.
     */
    short: string;

    /**
     * The name of the event.
     */
    name: string;

    /**
     * The hashtag used for promoting the event on social media.
     */
    hashtag: string;

    /**
     * The name of the charity being supported.
     */
    charityName: string;

    /**
     * The donation target amount.
     */
    targetAmount: number;

    /**
     * The minimum amount required to donate.
     */
    minimumDonation: number;

    /**
     * The currency used for donations.
     */
    paypalCurrency: string;

    /**
     * The start time in ISO 8601 format. Can be null if the event has no specified start time.
     */
    startTime: string | null;

    /**
     * The end time in ISO 8601 format. Can be null if the event has no specified end time.
     */
    endTime: string | null;

    /**
     * Whether the event is upcoming, in progress, or finished. Can be null if the status is not available.
     */
    timeStatus: TimeStatus | null;

    /**
     * The timezone of the event.
     */
    timezone: string;

    /**
     * Indicates whether editing is locked.
     */
    locked: boolean;

    /**
     * Indicates whether donations are allowed.
     */
    allowDonations: boolean;

    /**
     * The canonical link to the event on the donation tracker.
     */
    canonicalUrl: string;

    /**
     * The public display name of the event.
     */
    public: string;

    /**
     * The total amount raised.
     */
    amount: number;

    /**
     * The number of donations received.
     */
    count: number;

    /**
     * The largest donation received.
     */
    max: number;

    /**
     * The average donation received.
     */
    avg: number;

    /**
     * The URL to donate.
     */
    donationUrl: string;

    /**
     * The URL for the event's official schedule.
     */
    scheduleUrl: string;
}

/**
 * A run in an event.
 */
export interface Run {

    /**
     * The ID of the run on the donation tracker.
     * May be null if the run comes from a Horaro schedule and has no corresponding run on the donation tracker.
     */
    id: number | null;

    /**
     * The ID of the run on the Horaro schedule.
     * May be null if the event does not have a Horaro schedule.
     */
    horaroId: string | null;

    /**
     * The ID of the event that this run is associated with.
     */
    event: number;

    /**
     * The name of the run.
     * This is usually the name of the game being run, though is often prefixed with extra text like "BONUS GAME".
     */
    name: string;

    /**
     * The display name of the run.
     * This is usually the name of the game being run, though in recent years it has become very similar if not
     * identical to the `name` property.
     */
    displayName: string;

    /**
     * The name of the game on Twitch.
     * May be blank if the displayName already matches the name of the game on Twitch.
     */
    twitchName: string;

    /**
     * The console or platform the game is played on.
     */
    console: string;

    /**
     * The commentators for the run.
     */
    commentators: Headset[];

    /**
     * The hosts (i.e. donation readers) for the run.
     */
    hosts: Headset[];

    /**
     * The description of the run.
     * Rarely used.
     */
    description: string;

    /**
     * The start time of the run in ISO 8601 format.
     */
    startTime: string;

    /**
     * The end time of the run in ISO 8601 format.
     */
    endTime: string;

    /**
     * Whether the run is upcoming, in progress, or finished.
     */
    timeStatus: TimeStatus;

    /**
     * The length (or estimated length) of the run in h:mm:ss format.
     */
    runTime: string;

    /**
     * The setup time of the run in h:mm:ss format.
     */
    setupTime: string;

    /**
     * The order of the run in the schedule.
     */
    order: number;

    /**
     * Indicates whether a multiplayer run is a co-op run (true) or a race (false).
     */
    coop: boolean;

    /**
     * The speedrun category being run.
     */
    category: string;

    /**
     * The year the game was released. Can be null if the release year is not available.
     */
    releaseYear: number | null;

    /**
     * The runners performing this run.
     */
    runners: Runner[];

    /**
     * The runners performing this run as a pre-formatted string.
     */
    runnersAsString: string;

    /**
     * The bids (bid war parents and donation incentives) for this run.
     */
    bids: Bid[];

    /**
     * The VODs (Videos on Demand) for this run.
     */
    vods: VOD[];

    /**
     * The speedrun.com slug for the game being run. Can be null if the slug is not available.
     */
    src: string | null;
}


/**
 * Represents a runner of a speedrun.
 */
export interface Runner {

    /**
     * The name of the runner.
     */
    name: string;

    /**
     * The stream URL of the runner (usually a Twitch URL or handle).
     */
    stream: string;

    /**
     * The Twitter handle of the runner.
     */
    twitter: string;

    /**
     * The YouTube handle of the runner.
     */
    youtube: string;

    /**
     * The pronouns of the runner.
     */
    pronouns: string;

    /**
     * The public display name of the runner.
     */
    public: string;

    /**
     * The runner's primary social media URL.
     */
    url: string;
}

/**
 * A person with a headset (i.e. a commentator or a host).
 */
export interface Headset {

    /**
     * The name of the person.
     */
    name: string;

    /**
     * The pronouns of the person.
     */
    pronouns: string;
}

/**
 * Represents the state of a bid.
 */
export type BidState = 'OPENED' | 'CLOSED';

/**
 * A bid on a run.
 */
export interface Bid {

    /**
     * The child bid options for this bid (if this is a bid war).
     */
    children: Bid[];

    /**
     * The name of the bid.
     */
    name: string;

    /**
     * The state of the bid.
     */
    state: BidState;

    /**
     * The description of the bid.
     */
    description: string;

    /**
     * The one-line description of the bid.
     */
    shortDescription: string;

    /**
     * The goal amount for the bid. Can be null if no goal amount is specified (i.e. for non-donation incentives).
     */
    goal: number | null;

    /**
     * Indicates whether this bid is a donation incentive.
     */
    isTarget: boolean;

    /**
     * Indicates whether this bid allows users to enter their own options (i.e. for bid wars).
     */
    allowUserOptions: boolean;

    /**
     * The maximum length of a user-submitted string for this bid war.
     * Can be null if there is no maximum length specified.
     */
    optionMaxLength: number | null;

    /**
     * The time the bid was revealed in ISO 8601 format.
     * Can be null if the reveal time is not available.
     */
    revealedAt: string | null;

    /**
     * The total amount raised for this bid.
     */
    donationTotal: number;

    /**
     * The number of donations for this bid.
     */
    donationCount: number;

    /**
     * Indicates whether this bid is pinned to the stream overlay.
     */
    pinned: boolean;
}

/**
 * Represents the type of VOD.
 */
export type VODType = 'TWITCH' | 'YOUTUBE' | 'OTHER';

/**
 * Represents a VOD (Video on Demand).
 */
export interface VOD {

    /**
     * The type of VOD.
     */
    type: VODType;

    /**
     * The ID of the VOD video. Can be null if the ID is not available.
     */
    videoId: string | null;

    /**
     * The timestamp of the VOD. Can be null if the timestamp is not available.
     */
    timestamp: string | null;

    /**
     * The full URL of the VOD.
     */
    url: string;
}


// API Client

/**
 * A client for interacting with the vods.speedrun.club API.
 */
export class SvcClient {

    /**
     * The base URL of the service.
     */
    private readonly baseUrl: string;

    /**
     * Creates an instance of SvcClient.
     * @param baseUrl The base URL of the service. Defaults to the public instance 'https://vods.speedrun.club/api/v2'.
     */
    constructor(baseUrl: string = 'https://vods.speedrun.club/api/v2') {
        this.baseUrl = baseUrl;
    }

    /**
     * Performs a GET request to the specified URL and returns the response as a Promise.
     * @param url The URL to fetch data from.
     * @returns A Promise that resolves to the response data.
     */
    private get<T>(url: string | URL): Promise<T> {
        return fetch(`${this.baseUrl}/${url}`).then(res => res.json());
    }

    /**
     * Performs a GET request to the specified URL and returns the first element of the response as a Promise.
     * @param url The URL to fetch data from.
     * @returns A Promise that resolves to the first element of the response data.
     */
    private getFirst<T>(url: string | URL): Promise<T> {
        return this.get<T[]>(url).then(res => res[0]);
    }

    /**
     * Performs a GET request to the specified URL and returns the response as a Promise for a Map.
     * @param url The URL to fetch data from.
     * @returns A Promise that resolves to the response data as a Map.
     */
    private getAsMap<V>(url: string | URL): Promise<Map<string, V>> {
        return this.get<object>(url).then(res => new Map(Object.entries(res)));
    }

    /**
     * Retrieves all known marathon organizations, optionally with statistics.
     * @param stats Whether to include statistics. Defaults to true.
     * @returns A Promise that resolves to a Map object where the keys are organization IDs and the values are Organization objects.
     */
    getMarathons(stats: boolean = true): Promise<Map<String, Organization>> {
        return this.getAsMap(`marathons?stats=${stats}`);
    }

    /**
     * Retrieves a specific marathon by ID, optionally with statistics.
     * @param id The ID of the marathon to retrieve.
     * @param stats Whether to include statistics. Defaults to true.
     * @returns A Promise that resolves to the Organization object representing the marathon.
     */
    getMarathon(id: string, stats: boolean = true): Promise<Organization> {
        return this.get(`marathons/${id}?stats=${stats}`);
    }

    /**
     * Retrieves every event for every organization.
     * @returns A Promise that resolves to a Map object where the keys are organization IDs and the values are arrays of Event objects.
     */
    getAllEvents(): Promise<Map<string, Event[]>> {
        return this.getAsMap(`marathons/events`);
    }

    /**
     * Retrieves the events done by a specific organization.
     * @param organization The name of the organization.
     * @returns A Promise that resolves to an array of Event objects representing the events.
     */
    getEvents(organization: string): Promise<Event[]> {
        return this.get(`marathons/${organization}/events`);
    }

    /**
     * Retrieves a specific event by organization and event ID.
     * @param organization The name of the organization.
     * @param event The ID of the event to retrieve.
     * @returns A Promise that resolves to the Event object representing the event.
     */
    getEvent(organization: string, event: string): Promise<Event> {
        return this.getFirst(`marathons/${organization}/events?id=${event}`);
    }

    /**
     * Retrieves the runs for a specific organization, event, and runner.
     * @param organization The name of the organization.
     * @param event Optional. The ID of the event to filter runs. Defaults to undefined.
     * @param runner Optional. The ID of the runner to filter runs. Defaults to undefined.
     * @returns A Promise that resolves to an array of Run objects representing the runs.
     */
    getRuns(organization: string, event?: string, runner?: number): Promise<Run[]> {
        const url = new URL(`marathons/${organization}/runs`, this.baseUrl);
        if (event) url.searchParams.append('event', event);
        if (runner) url.searchParams.append('runner', runner.toString());
        return this.get(url);
    }

    /**
     * Retrieves a specific run by organization and run ID.
     * @param organization The name of the organization.
     * @param id The ID of the run to retrieve.
     * @returns A Promise that resolves to the Run object representing the run.
     */
    getRun(organization: string, id: string): Promise<Run> {
        return this.getFirst(`marathons/${organization}/runs?id=${id}`);
    }

    /**
     * Creates an instance of MarathonClient associated with a specific organization.
     * @param organization The name of the organization.
     * @returns A MarathonClient instance.
     */
    getMarathonClient(organization: string): MarathonClient {
        return new MarathonClient(this, organization);
    }
}

/**
 * Represents a client for interacting with a specific marathon.
 */
export class MarathonClient {
    private readonly svc: SvcClient;
    private readonly organization: string;

    /**
     * Creates an instance of MarathonClient associated with a specific organization.
     * @param svc The SvcClient instance.
     * @param organization The name of the organization.
     */
    constructor(svc: SvcClient, organization: string) {
        this.svc = svc;
        this.organization = organization;
    }

    /**
     * Retrieves the organization details for the marathon.
     * @returns A Promise that resolves to the Organization object representing the marathon.
     */
    get(): Promise<Organization> {
        return this.svc.getMarathon(this.organization);
    }

    /**
     * Retrieves the events associated with the marathon.
     * @returns A Promise that resolves to an array of Event objects representing the events.
     */
    getEvents(): Promise<Event[]> {
        return this.svc.getEvents(this.organization);
    }

    /**
     * Retrieves a specific event by event ID associated with the marathon.
     * @param event The ID of the event to retrieve.
     * @returns A Promise that resolves to the Event object representing the event.
     */
    getEvent(event: string): Promise<Event> {
        return this.svc.getEvent(this.organization, event);
    }

    /**
     * Retrieves the runs associated with the marathon.
     * @param event Optional. The ID of the event to filter runs. Defaults to undefined.
     * @param runner Optional. The ID of the runner to filter runs. Defaults to undefined.
     * @returns A Promise that resolves to an array of Run objects representing the runs.
     */
    getRuns(event?: string, runner?: number): Promise<Run[]> {
        return this.svc.getRuns(this.organization, event, runner);
    }

    /**
     * Retrieves a specific run by run ID associated with the marathon.
     * @param id The ID of the run to retrieve.
     * @returns A Promise that resolves to the Run object representing the run.
     */
    getRun(id: string): Promise<Run> {
        return this.svc.getRun(this.organization, id);
    }
}

/**
 * An instance of the SvcClient class using the default base URL.
 */
export const svc = new SvcClient();
