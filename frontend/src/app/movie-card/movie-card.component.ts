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
import { SessionService } from '../session.service';

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
  newSeed: string = '';

  constructor(private http: HttpClient, private dialog: MatDialog, private sessionService: SessionService) {
  }

  ngOnInit(): void {
    this.limit = this.movies.length;
    console.log(this.limit)
  }

  skipNext() {
    this.index++;
    if (this.index % (this.limit / 2) == 0) {
      this.fetchMoreMovies();
    }
  }

  favorite(movieId: number) {
    const url = `${environment.apiBaseUrl}/api/v1/session/add-to-likes`

    this.http.post<Boolean>(url, {
      "seed": this.seed,
      "playerSessionId": this.sessionService.getSessionId(),
      "movieId": movieId
    }).subscribe({
      next: response => {
        console.log('Response: ' + response);
        if (response === true) {
          console.log("Common like!")//TODO: add logic for common likes; maybe cache in browser the details about the common likes
        }
      },
      error: error => {
        console.error('API Error:', error);
      }
    })

    this.skipNext();
  }

  fetchMoreMovies() {
    const url = `${environment.apiBaseUrl}/api/v1/session/fetch-more`;

    this.http.post<Movie[]>(url, {
      "seed": this.seed,
      "playerSessionId": this.sessionService.getSessionId(),
      "language": "en-US"//TODO: make this configurable
    }).subscribe({
      next: response => {
        this.movies.push(...response);
        console.log('New List:', this.movies);
      },
      error: error => {
        console.error('API Error:', error);
      }
    })

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
