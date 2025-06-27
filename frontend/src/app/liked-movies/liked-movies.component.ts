import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { LocalStorageService } from '../local-storage.service';
import { Movie } from '../models/movie.model';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TrailerDialogComponent } from '../movie-card/trailer-dialog.component';
import { HttpClient } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { NoTrailerDialogComponent } from '../movie-card/no-trailer-dialog.component';
import { MatExpansionModule } from '@angular/material/expansion';

@Component({
  selector: 'app-liked-movies',
  imports: [
    CommonModule,

    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatExpansionModule,
  ],
  templateUrl: './liked-movies.component.html',
  styleUrl: './liked-movies.component.css'
})
export class LikedMoviesComponent implements OnInit {
  @Input() playerLikedMovies: Movie[] = [];
  @Input() commonLikedMovies: Movie[] = [];
  @Output() return = new EventEmitter<void>();

  seed: String = '';
  displayBigMovieCard: boolean = false;
  movieToShow: Movie | null = null;

  constructor(private localStorageService: LocalStorageService, private http: HttpClient, private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.seed = this.localStorageService.getSeed();
  }

  back() {
    this.return.emit();
  }

  onClickShowCard(movieId: number, playerLiked: boolean) {
    if (playerLiked === true) {
      this.movieToShow = this.playerLikedMovies.find(playerLikedMovie => playerLikedMovie.id === movieId) || null;
    }
    else if (playerLiked === false) {
      this.movieToShow = this.commonLikedMovies.find(commonLikedMovie => commonLikedMovie.id === movieId) || null;
    }
    this.displayBigMovieCard = true;
  }

  openYoutubeTrailerDialog(movieId: number) {
    this.getYoutubeTrailer(movieId).subscribe({
      next: (ytTrailer) => {
        if (ytTrailer != null) {
          // Show trailer if found
          this.dialog.open(TrailerDialogComponent, {
            data: ytTrailer,
            width: '95vw',
            height: '40vh',
          });
        } else {
          // Fallback dialog if no trailer
          this.dialog.open(NoTrailerDialogComponent);
        }
      },
      error: (error) => {
        // Handle 404 separately
        if (error.status === 404) {
          this.dialog.open(NoTrailerDialogComponent);
        } else {
          console.error('API Error:', error);
        }
      }
    });
  }

  getYoutubeTrailer(movieId: number): Observable<string> {
    const url = `${environment.apiBaseUrl}/api/v1/tmdb/youtube-trailer/${movieId}/en-US`; // TODO: add lang config

    return this.http.get(url, { responseType: 'text' }); // Returns plain text (YouTube URL)
  }
}
