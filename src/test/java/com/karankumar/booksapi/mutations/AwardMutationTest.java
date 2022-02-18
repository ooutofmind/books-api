/*
 * Copyright (C) 2021  Karan Kumar
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.karankumar.booksapi.mutations;

import com.acme.DgsConstants;
import com.karankumar.booksapi.model.Book;
import com.karankumar.booksapi.model.PublishingFormat;
import com.karankumar.booksapi.model.award.Award;
import com.karankumar.booksapi.model.award.AwardName;
import com.karankumar.booksapi.model.genre.Genre;
import com.karankumar.booksapi.model.genre.GenreName;
import com.karankumar.booksapi.model.language.Lang;
import com.karankumar.booksapi.model.language.LanguageName;
import com.karankumar.booksapi.service.AwardService;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static com.karankumar.booksapi.mutations.AwardMutation.NOT_FOUND_ERROR_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AwardMutationTest {

    @MockBean
    private AwardService awardService;

    @Autowired
    private AwardMutation underTest;

    @MockBean
    private DataFetchingEnvironment dataFetchingEnvironment;

    @Test
    public void addAward_shouldSaveAward() {
        // given
        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.AwardName)).thenReturn("PORTICO_PRIZE");
        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.Category)).thenReturn("test");
        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.Year)).thenReturn(1994);
        ArgumentCaptor<Award> awardArgumentCaptor = ArgumentCaptor.forClass(Award.class);

        // when
        underTest.addAward(dataFetchingEnvironment);

        // then
        verify(awardService).save(awardArgumentCaptor.capture());
        assertSoftly(softly -> {
            softly.assertThat(awardArgumentCaptor.getValue().getAwardName()).isEqualTo(AwardName.PORTICO_PRIZE);
            softly.assertThat(awardArgumentCaptor.getValue().getCategory()).isEqualTo("test");
            softly.assertThat(awardArgumentCaptor.getValue().getYear()).isEqualTo(1994);
        });
    }

    @Test
    public void addAward_throws_whenAwardNameWasNotFound() {
        // given
        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.AwardName)).thenReturn("DUMMY");
        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.Category)).thenReturn("drama");
        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.Year)).thenReturn(2005);

        // when
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> underTest.addAward(dataFetchingEnvironment));

        // then
        assertSoftly(softly -> {
            softly.assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            softly.assertThat(exception.getReason()).isEqualTo(AwardMutation.NOT_FOUND_ERROR_MESSAGE);
        });
    }

    @Test
    public void deleteAward_throws_whenIdWasNotFound() {
        // given
        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.Id)).thenReturn(null);

        // when/then
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> underTest.deleteAward(dataFetchingEnvironment));

        assertSoftly(softly -> {
            softly.assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            softly.assertThat(exception.getReason()).isEqualTo(NOT_FOUND_ERROR_MESSAGE);
        });
    }

    @Test
    public void deleteAward_throws_whenAwardWasNotFound() {
        // given
        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.Id)).thenReturn("1");
        when(awardService.findById(1L)).thenReturn(Optional.empty());

        // when/then
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> underTest.deleteAward(dataFetchingEnvironment));

        verify(awardService, times(1)).findById(1L);

        assertSoftly(softly -> {
            softly.assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            softly.assertThat(exception.getReason()).isEqualTo(NOT_FOUND_ERROR_MESSAGE);
        });
    }

    @Test
    public void deleteAward_shouldReturnDeletedAward() {
        // given
        Book book = new Book(
                "TestTitle",
                new Lang(LanguageName.AFRIKAANS),
                "Blurb",
                new Genre(GenreName.ADVENTURE),
                new PublishingFormat(PublishingFormat.Format.HARDCOVER)
        );
        Award award = new Award(AwardName.ORWELL_PRIZE,"test",2010, new HashSet<>(Collections.singletonList(book)));

        when(dataFetchingEnvironment.getArgument(DgsConstants.AWARD.Id)).thenReturn("1");
        when(awardService.findById(1L)).thenReturn(Optional.of(award));

        // when
        Award resultAward = underTest.deleteAward(dataFetchingEnvironment);

        // then
        verify(awardService, times(1)).deleteAward(award);

        assertThat(resultAward).isSameAs(award);
    }
}
