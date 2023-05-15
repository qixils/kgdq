export type TimeStatus = 'UPCOMING' | 'IN_PROGRESS' | 'FINISHED';

export interface Event {
    id: number,
    // event slug / short name
    short: string,
    name: string,
    hashtag: string,
    charityName: string,
    // donation target
    targetAmount: number,
    // minimum donation amount
    minimumDonation: number,
    paypalCurrency: string,
    // start time of event in ISO 8601 format
    startTime: string,
    // end time of event in ISO 8601 format
    endTime: string | null,
    // status of event
    timeStatus: TimeStatus | null,
    // timezone of event
    timezone: string,
    // whether editing is locked
    locked: boolean,
    // whether donations are allowed
    allowDonations: boolean,
    // public URL of event
    canonicalUrl: string,
    // public display name
    public: string,
    // total amount raised
    amount: number,
    // number of donations
    count: number,
    // largest donation
    max: number,
    // average donation
    avg: number,
}

export interface Run {
    id: number | null,
    horaroId: string | null,
    // ID of the event this run is for
    event: number,
    // name of the run
    name: string,
    // generally the name of the game
    displayName: string,
    // the name of the game on Twitch
    twitchName: string,
    // the console the game is played on
    console: string,
    // the commentators for the run
    commentators: Headset[],
    // the hosts for the run
    hosts: Headset[],
    // the description of the run
    description: string,
    // the start time of the run in ISO 8601 format
    startTime: string | null,
    // the end time of the run in ISO 8601 format
    endTime: string | null,
    // the status of the run in the schedule
    timeStatus: TimeStatus | null,
    // the run time (or estimated run time) of the run in h:mm:ss format
    runTime: string,
    // the setup time of the run in h:mm:ss format
    setupTime: string,
    // the order of the run in the schedule
    order: number,
    // whether a multiplayer run is a co-op run (true) or a race (false)
    coop: boolean,
    // the speedrun category being run
    category: string,
    // the year the game released
    releaseYear: number | null,
    // the runners performing this run
    runners: Runner[],
    // the runners performing this run as a pre-formatted string
    runnersAsString: string,
    // the bids (bid wars, donation incentives) for this run
    bids: Bid[],
    // the VODs for this run
    vods: VOD[],
    // the speedrun.com slug for the game being run
    src: string | null,
}

export interface Runner {
    // the name of the runner
    name: string,
    // the stream URL of the runner
    stream: string,
    // the Twitter handle of the runner
    twitter: string,
    // the YouTube channel name of the runner
    youtube: string,
    // the pronouns of the runner
    pronouns: string,
    // the public display name of the runner
    public: string,
    // the runner's primary social media URL
    url: string,
}

// a person with a headset
export interface Headset {
    // their name
    name: String,
    // their pronouns
    pronouns: String,
}

export type BidState = 'OPENED' | 'CLOSED';

export interface Bid {
    // the child bid options for this bid (if this is a bid war)
    children: Bid[],
    // the name of the bid
    name: string,
    // the state of the bid
    state: BidState,
    // the description of the bid
    description: string,
    // the one-line description of the bid
    shortDescription: string,
    // the goal amount for the bid
    goal: number | null,
    // whether this bid is a donation incentive
    isTarget: boolean,
    // whether this bid allows users to enter their own options
    allowUserOptions: boolean,
    // the maximum length of a bid option
    optionMaxLength: number | null,
    // the time the bid was revealed in ISO 8601 format
    revealedAt: string | null,
    // the total amount raised for this bid
    donationTotal: number,
    // the number of donations for this bid
    donationCount: number,
    // whether this bid is pinned to the stream overlay
    pinned: boolean,
}

export type VODType = 'TWITCH' | 'YOUTUBE' | 'OTHER';

export interface VOD {
    type: VODType, // the type of VOD
    videoId: string | null, // the ID of the VOD video
    timestamp: string | null, // the timestamp of the VOD
    url: string, // the full URL of the VOD
}