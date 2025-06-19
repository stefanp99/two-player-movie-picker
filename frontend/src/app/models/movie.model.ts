export interface Genre {
    id: number;
    name: string;
}

export interface Movie {
    backDropUrl: string | null;
    genres: Genre[];
    id: number;
    imdbUrl: string;
    overview: string;
    popularity: number;
    posterUrl: string;
    releaseDate: string;
    runtime: number;
    spokenLanguages: string[];
    status: string;
    tagline: string;
    title: string;
    tmdbUrl: string;
    voteAverage: number;
    voteCount: number;
}
