// An organization that hosts speedrunning events.
export interface Organization {
    // The text ID of the organization, e.g. `gdq`
    id: string

    displayName: string;
    shortName: string;
    homepageUrl: string;

    /**
     * Indicates whether this organization supports automatic VOD (Video on Demand) link generation.
     */
    autoVODs: boolean;
}

export interface OrganizationWithStats extends Organization {
    amountRaised: number;
    donationCount: number;
}

// The status of an event or run.
export type TimeStatus = 'UPCOMING' | 'IN_PROGRESS' | 'FINISHED';

// An event being hosted by a speedrunning organization.
export interface MarathonEvent {

    // The positive integer ID of the event.
    id: string;

    // The event slug or short name.
    short: string;

    // The full name of the event.
    name: string;

    // The start time in ISO 8601 format. Can be null if the event has no specified start time.
    startTime: string | null;

    // The end time in ISO 8601 format. Can be null if the event has no specified end time.
    endTime: string | null;

    timezone: string | null;

    // The total amount raised.
    amount: number;

    // The total number of donations received.
    count: number | null;

    //The name of the charity being supported.
    charityName: string;

    // The currency used for donations.
    currency: string;

    // Can be null if the status is not available.
    timeStatus: TimeStatus | null;

    // The URL to donate.
    donationUrl: string;

    // The URL for the event's official schedule.
    scheduleUrl: string;

    // The text ID of the organization that owns this event, e.g. `gdq`
    organization: string;
}

export interface Run {

    // The ID of the run.
    id: string | null;

    // The name of the run.
    // This is usually the name of the game being run, though is often prefixed with extra text like `"BONUS GAME"`.
    name: string;

    // This is usually the name of the game being run, though in recent years it has become very similar if not
    // identical to the `name` property.
    displayName: string;

    // The name of the game on Twitch.
    // May be blank if the displayName already matches the name of the game on Twitch.
    twitchName: string;

    // The console or platform the game is played on.
    console: string;

    // The year the game was released. Can be null if the release year is not available.
    releaseYear: number | null;

    // The description of the run. Increasingly used for meta-"runs" like the opening ceremony.
    // Uses markdown.
    description: string;

    // The start time of the run in ISO 8601 format.
    startTime: string;

    // The end time of the run in ISO 8601 format.
    endTime: string;

    // The length (or estimated length) of the run in h:mm:ss format.
    runTime: string;

    // The setup time of the run in h:mm:ss format.
    setupTime: string;

    // Indicates whether a multiplayer run is a co-op run (true) or a race (false).
    coop: boolean;

    // The speedrun category being run.
    category: string;

    // The runners performing this run.
    runners: Talent[];

    // The commentators for the run.
    commentators: Talent[];

    // The hosts (i.e. donation readers) for the run.
    hosts: Talent[];

    // The bids (bid war parents and donation incentives) for this run.
    bids: Bid[];

    // The VODs (Videos on Demand) for this run.
    vods: VOD[];

    // The speedrun.com slug for the game being run. Can be null if the slug is not available.
    src: string | null;

    // Whether the run is upcoming, in progress, or finished.
    timeStatus: TimeStatus;
}


// Represents a runner of a speedrun.
export interface Talent {

    // The name of the runner.
    name: string;

    // The stream URL of the runner (usually a Twitch URL or handle).
    stream: string;

    // The Twitter handle of the runner.
    twitter: string;

    // The YouTube handle of the runner.
    youtube: string;

    // The pronouns of the runner.
    pronouns: string;

    // The public display name of the runner.
    public: string;

    // The runner's primary social media URL.
    url: string;
}

// Represents the state of a bid.
export type BidState = 'OPENED' | 'CLOSED';

// A bid on a run.
export interface Bid {

    id: string;

    // The child bid options for this bid (if this is a bid war).
    children: Bid[];

    // The name of the bid.
    name: string;

    // The state of the bid.
    state: BidState;

    // The description of the bid.
    description: string;

    // The one-line description of the bid.
    shortDescription: string;

    // The goal amount for the bid. Can be null if no goal amount is specified (i.e. for non-donation incentives).
    goal: number | null;

    // Indicates whether this bid is a donation incentive.
    isTarget: boolean;

    // Indicates whether this bid allows users to enter their own options (i.e. for bid wars).
    allowUserOptions: boolean;

    // The maximum length of a user-submitted string for this bid war.
    // Can be null if there is no maximum length specified.
    optionMaxLength: number | null;

    // The total amount raised for this bid.
    donationTotal: number;

    // The number of donations for this bid.
    donationCount: number;
}

// Represents the type of VOD.
export type VODType = 'TWITCH' | 'YOUTUBE' | 'OTHER';

// Represents a VOD (Video on Demand).
export interface VOD {

    // The type of VOD.
    type: VODType;

    // The ID of the VOD video. Can be null if the ID is not available.
    videoId: string | null;

    // The timestamp of the VOD. Can be null if the timestamp is not available.
    timestamp: string | null;

    // The full URL of the VOD.
    url: string;

    // ID of the SVC user who submitted this VOD.
    contributorId: string | null;
}


// A client for interacting with the vods.speedrun.club API.
export class SvcClient {

    // The base URL of the service.
    private readonly baseUrl: string;

    /**
     * The function to use to fetch URLs.
     */
    private readonly fetchFunction: typeof fetch;

    /** Creates an instance of SvcClient.
     * @param baseUrl The base URL of the service. Defaults to the public instance 'https://vods.speedrun.club/api/v2'.
     * @param fetchFunction The function to use to fetch URLs. Defaults to the global fetch function.
     */
    constructor(baseUrl: string = 'https://vods.speedrun.club/api/v2', fetchFunction: typeof fetch = fetch) {
        this.baseUrl = baseUrl;
        this.fetchFunction = fetchFunction;
    }

    /** Performs a GET request to the specified URL and returns the response as a Promise.
     * @param url The URL to fetch data from.
     * @returns A Promise that resolves to the response data.
     */
    private async get<T>(url: string | URL): Promise<T> {
        // prepend the base URL if the URL is relative (i.e. a string)
        const calculatedUrl = typeof url === 'string' ? `${this.baseUrl}/${url}` : url;
        const res = await this.fetchFunction(calculatedUrl);
        if (res.status >= 400) {
            throw new Error(`Request failed with status code ${res.status}`);
        }
        return await res.json();
    }

    /** Retrieves all known marathon organizations, optionally with statistics.
     * @param stats Whether to include statistics. Defaults to true.
     * @returns A Promise that resolves to a Map object where the keys are organization IDs and the values are Organization objects.
     */
    async getMarathons(stats?: false): Promise<Organization[]>
    async getMarathons(stats?: true): Promise<OrganizationWithStats[]>
    async getMarathons(stats: boolean = true): Promise<Organization[] | OrganizationWithStats[]> {
        let json = await this.get<object>(`marathons?stats=${stats}`);
        return Object.entries(json).map(([id, org]) => ({ id, ...org }));
    }

    /** Retrieves a specific marathon by ID, optionally with statistics.
     * @param id The ID of the marathon to retrieve.
     * @param stats Whether to include statistics. Defaults to true.
     * @returns A Promise that resolves to the Organization object representing the marathon.
     */
    async getMarathon(id: string, stats?: false): Promise<Organization>
    async getMarathon(id: string, stats?: true): Promise<OrganizationWithStats>
    async getMarathon(id: string, stats: boolean = true): Promise<Organization|OrganizationWithStats> {
        return { id,  ...await this.get<any>(`marathons/${id}?stats=${stats}`) };
    }

    /** Retrieves every event for every organization.
     * @returns A Promise that resolves to a Map object where the keys are organization IDs and the values are arrays of Event objects.
     */
    async getAllEvents(): Promise<MarathonEvent[]> {
        let json = await this.get<object>('marathons/events');
        return Object.entries(json).map(([organization, events]) => (events as any[]).map(event => ({ organization, ...event }))).flat();
    }

    /** Retrieves the events done by a specific organization.
     * @param organization The name of the organization.
     * @returns A Promise that resolves to an array of Event objects representing the events.
     */
    async getEvents(organization: string): Promise<MarathonEvent[]> {
        let json = await this.get<Omit<MarathonEvent, 'organization'>[]>(`marathons/${organization}/events`);
        return json.map(event => ({ organization, ...event}));
    }

    /** Retrieves a specific event by organization and event ID.
     * @param organization The name of the organization.
     * @param event The ID of the event to retrieve.
     * @returns A Promise that resolves to the Event object representing the event.
     */
    async getEvent(organization: string, event: string, skipLoad: boolean = false): Promise<MarathonEvent> {
        let json = await this.get<Omit<MarathonEvent, 'organization'>>(`marathons/${organization}/events/${event}?skipLoad=${skipLoad}`);
        return { organization, ...json };
    }

    /** Retrieves the runs for a specific organization, event, and runner.
     * @param organization The name of the organization.
     * @param event Optional. The ID of the event to filter runs. Defaults to undefined.
     * @param runner Optional. The ID of the runner to filter runs. Defaults to undefined.
     * @returns A Promise that resolves to an array of Run objects representing the runs.
     */
    getRuns(organization: string, event?: string, runner?: number): Promise<Run[]> {
        let url = new URL(`marathons/${organization}/runs`, this.baseUrl + '/');
        if (event) url = new URL(`marathons/${organization}/events/${event}/runs`, this.baseUrl + '/');
        if (runner) url.searchParams.append('runner', runner.toString());
        return this.get(url);
    }

    /** Retrieves a specific run by organization and run ID.
     * @param organization The name of the organization.
     * @param id The ID of the run to retrieve.
     * @returns A Promise that resolves to the Run object representing the run.
     */
    async getRun(organization: string, id: string): Promise<Run> {
        return (await this.get<Run[]>(`marathons/${organization}/runs/${id}`))[0];
    }

    /** Creates an instance of MarathonClient associated with a specific organization.
     * @param organization The name of the organization.
     * @returns A MarathonClient instance.
     */
    getMarathonClient(organization: string): MarathonClient {
        return new MarathonClient(this, organization);
    }
}

// Represents a client for interacting with a specific marathon.
export class MarathonClient {
    private readonly svc: SvcClient;
    private readonly organization: string;

    /** Creates an instance of MarathonClient associated with a specific organization.
     * @param svc The SvcClient instance.
     * @param organization The name of the organization.
     */
    constructor(svc: SvcClient, organization: string) {
        this.svc = svc;
        this.organization = organization;
    }

    /** Retrieves the organization details for the marathon.
     * @returns A Promise that resolves to the Organization object representing the marathon.
     */
    get(): Promise<Organization> {
        return this.svc.getMarathon(this.organization);
    }

    /** Retrieves the events associated with the marathon.
     * @returns A Promise that resolves to an array of Event objects representing the events.
     */
    getEvents(): Promise<MarathonEvent[]> {
        return this.svc.getEvents(this.organization);
    }

    /** Retrieves a specific event by event ID associated with the marathon.
     * @param event The ID of the event to retrieve.
     * @returns A Promise that resolves to the Event object representing the event.
     */
    getEvent(event: string): Promise<MarathonEvent> {
        return this.svc.getEvent(this.organization, event);
    }

    /** Retrieves the runs associated with the marathon.
     * @param event Optional. The ID of the event to filter runs. Defaults to undefined.
     * @param runner Optional. The ID of the runner to filter runs. Defaults to undefined.
     * @returns A Promise that resolves to an array of Run objects representing the runs.
     */
    getRuns(event?: string, runner?: number): Promise<Run[]> {
        return this.svc.getRuns(this.organization, event, runner);
    }

    /** Retrieves a specific run by run ID associated with the marathon.
     * @param id The ID of the run to retrieve.
     * @returns A Promise that resolves to the Run object representing the run.
     */
    getRun(id: string): Promise<Run> {
        return this.svc.getRun(this.organization, id);
    }
}

const svc = new SvcClient();
export { svc };
