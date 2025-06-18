import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Movie } from '../models/movie.model';
import { NoTrailerDialogComponent } from './no-trailer-dialog.component';
import { TrailerDialogComponent } from './trailer-dialog.component';

@Component({
  selector: 'app-movie-card',
  imports: [
    CommonModule,

    MatCardModule,
    MatGridListModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule
  ],
  templateUrl: './movie-card.component.html',
  styleUrl: './movie-card.component.css'
})
export class MovieCardComponent implements OnInit {
  @Input() movies: Movie[] = [];
  @Input() seed: string = '';
  index: number = 0;
  limit: number = 0;

  constructor(private http: HttpClient, private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.limit = this.movies.length;
    console.log(this.limit)
  }

  skipNext() {
    this.index++;
    if (this.index % (this.limit / 2) == 0) {
      this.generateNewSeedAndLoadMoreMovies();
    }
  }

  favorite() {
    this.index++;
    if (this.index % (this.limit / 2) == 0) {
      this.generateNewSeedAndLoadMoreMovies();
    }
  }

  generateNewSeedAndLoadMoreMovies() {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/generate-seed/${this.seed}`;

    this.http.get(url, { responseType: 'text' }).subscribe({
      next: response => {
        this.seed = response;
        console.log('New Seed:', this.seed);
        this.loadMoreMovies(this.limit / 2);
      },
      error: error => {
        console.error('API Error:', error);
      }
    })

  }

  loadMoreMovies(limit: number) {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/fetch?language=en-US&limit=${limit}&seed=${this.seed}`;//TODO: make lang configurable

    this.http.get<Movie[]>(url).subscribe({
      next: response => {
        this.movies.push(...response);
        console.log('New List:', this.movies);
      },
      error: error => {
        console.error('API Error:', error);
      }
    });
  }

  openYoutubeTrailerDialog(movieId: number) {
    this.getYoutubeTrailer(movieId).subscribe({
      next: (ytTrailer) => {
        if (ytTrailer != null) {
          this.dialog.open(TrailerDialogComponent, {
            data: ytTrailer,
            width: '50vw',
            height: '50vh',
          });
        } else {
          this.dialog.open(NoTrailerDialogComponent);
        }
      },
      error: (error) => {
        if (error.status === 404) {
          this.dialog.open(NoTrailerDialogComponent);
        } else {
          console.error('API Error:', error);
        }
      }
    });
  }

  getYoutubeTrailer(movieId: number): Observable<string> {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/youtube-trailer/${movieId}/en-US`;//TODO: add lang config

    return this.http.get(url, { responseType: 'text' });
  }


}
