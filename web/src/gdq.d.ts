export interface Event {
    id: number,
    short: string, // event slug / short name
    name: string,
    hashtag: string,
    charityName: string,
    targetAmount: number, // donation target
    minimumDonation: number, // minimum donation amount
    paypalCurrency: string,
    startTime: string, // start time of event in ISO 8601 format
    endTime: string, // end time of event in ISO 8601 format
    timeStatus: string, // status of event (either UPCOMING, IN_PROGRESS, or FINISHED)
    timezone: string, // timezone of event
    locked: boolean, // whether editing is locked
    allowDonations: boolean, // whether donations are allowed
    canonicalUrl: string, // public URL of event
    public: string, // public display name
    amount: number, // total amount raised
    count: number, // number of donations
    max: number, // largest donation
    avg: number, // average donation
}

export interface Run {
    id: number | null,
    horaroId: string | null,
    event: number, // ID of the event this run is for
    name: string, // name of the run
    displayName: string, // generally the name of the game
    twitchName: string, // the name of the game on Twitch
    console: string, // the console the game is played on
    commentators: Headset[], // the commentators for the run
    hosts: Headset[], // the hosts for the run
    description: string, // the description of the run
    startTime: string | null, // the start time of the run in ISO 8601 format
    endTime: string | null, // the end time of the run in ISO 8601 format
    runTime: string, // the run time (or estimated run time) of the run in h:mm:ss format
    setupTime: string, // the setup time of the run in h:mm:ss format
    order: number, // the order of the run in the schedule
    coop: boolean, // whether a multiplayer run is a co-op run (true) or a race (false)
    category: string, // the speedrun category being run
    releaseYear: number | null, // the year the game released
    runners: Runner[], // the runners performing this run
    runnersAsString: string, // the runners performing this run as a pre-formatted string
    bids: Bid[], // the bids (bid wars, donation incentives) for this run
    vods: VOD[], // the VODs for this run
    src: string | null, // the speedrun.com slug for the game being run
    timeStatus: string | null, // the status of the run in the schedule
}

export interface Runner {
    name: string, // the name of the runner
    stream: string, // the stream URL of the runner
    twitter: string, // the Twitter handle of the runner
    youtube: string, // the YouTube channel name of the runner
    pronouns: string, // the pronouns of the runner
    public: string, // the public display name of the runner
    url: string, // the runner's primary social media URL
}

export interface Headset {
    name: String, // the name of a person with a headset
    pronouns: String, // their pronouns
}

export interface Bid {
    children: Bid[], // the child bid options for this bid (if this is a bid war)
    name: string, // the name of the bid
    state: string, // the state of the bid (either 'OPENED' or 'CLOSED')
    description: string, // the description of the bid
    shortDescription: string, // the one-line description of the bid
    goal: number | null, // the goal amount for the bid
    isTarget: boolean, // whether this bid is a donation incentive
    allowUserOptions: boolean, // whether this bid allows users to enter their own options
    optionMaxLength: number | null, // the maximum length of a bid option
    revealedAt: string | null, // the time the bid was revealed in ISO 8601 format
    donationTotal: number, // the total amount raised for this bid
    donationCount: number, // the number of donations for this bid
    pinned: boolean, // whether this bid is pinned to the stream overlay
}

export interface VOD {
    type: string, // the type of VOD (either 'TWITCH', 'YOUTUBE', or 'OTHER')
    videoId: string | null, // the ID of the VOD video
    timestamp: string | null, // the timestamp of the VOD
    url: string, // the full URL of the VOD
}