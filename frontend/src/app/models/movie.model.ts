import { Genre } from "./genre.model";

export interface Movie {
    backdropUrl: string | null;
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
