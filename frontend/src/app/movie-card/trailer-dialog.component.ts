import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-trailer-dialog',
  standalone: true,
  imports: [MatDialogModule],
  template: `
    <mat-dialog-content>
      <div class="videoWrapper">
        <iframe [src]="ytUrl" frameborder="0" allowfullscreen></iframe>
      </div>
    </mat-dialog-content>
  `,
  styles: [`
    .videoWrapper {
      width: 100%;
      height: 100%;
    }

    iframe {
      width: 100%;
      height: 100%;
    }
  `]
})
export class TrailerDialogComponent {
  ytUrl: SafeResourceUrl;

  constructor(@Inject(MAT_DIALOG_DATA) data: string, private sanitizer: DomSanitizer) {
    this.ytUrl = this.sanitizer.bypassSecurityTrustResourceUrl(data);
  }
}
