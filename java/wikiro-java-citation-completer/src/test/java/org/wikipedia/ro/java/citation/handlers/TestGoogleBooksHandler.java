package org.wikipedia.ro.java.citation.handlers;

import java.util.Collections;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.api.services.books.v1.Books;
import com.google.api.services.books.v1.model.Volume;
import com.google.api.services.books.v1.model.Volume.VolumeInfo;
import com.google.api.services.books.v1.model.Volumes;

public class TestGoogleBooksHandler {

    @Test
    public void testGetGoogleBooksTitle() throws Exception {
        Books mockBooks = Mockito.mock(Books.class);
        Books.Volumes mockVolumes = Mockito.mock(Books.Volumes.class);
        Books.Volumes.Get mockGet = Mockito.mock(Books.Volumes.Get.class);

        Volume mockVolume = new Volume();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setTitle("Test Book");
        mockVolume.setVolumeInfo(volumeInfo);

        Mockito.when(mockBooks.volumes()).thenReturn(mockVolumes);
        Mockito.when(mockVolumes.get("OfywzQEACAAJ")).thenReturn(mockGet);
        Mockito.when(mockGet.execute()).thenReturn(mockVolume);

        GoogleBooksHandler sut = new GoogleBooksHandler(mockBooks);
        Optional<String> citation =
            sut.processCitationParams("https://books.google.ro/books?id=OfywzQEACAAJ&hl=ro&sa=X&redir_esc=y");

        Assert.assertTrue("Citation should be present", citation.isPresent());
    }

    @Test
    public void testGetGoogleBooksTitleWithKeywords() throws Exception {
        Books mockBooks = Mockito.mock(Books.class);
        Books.Volumes mockVolumes = Mockito.mock(Books.Volumes.class);
        Books.Volumes.Get mockGet = Mockito.mock(Books.Volumes.Get.class);

        Volume mockVolume = new Volume();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setTitle("Test Book");
        mockVolume.setVolumeInfo(volumeInfo);

        Mockito.when(mockBooks.volumes()).thenReturn(mockVolumes);
        Mockito.when(mockVolumes.get("OfywzQEACAAJ")).thenReturn(mockGet);
        Mockito.when(mockGet.execute()).thenReturn(mockVolume);

        GoogleBooksHandler sut = new GoogleBooksHandler(mockBooks);
        Optional<String> citation = sut
            .processCitationParams("https://books.google.ro/books?id=OfywzQEACAAJ&dq=test+keywords&hl=ro&sa=X&redir_esc=y");

        Assert.assertTrue("Citation should be present", citation.isPresent());
        Assert.assertTrue("Citation should contain keywords parameter", citation.get().contains("keywords=test+keywords"));
    }

    @Test
    public void testGetGoogleBooksTitleByIsbn() throws Exception {
        Books mockBooks = Mockito.mock(Books.class);
        Books.Volumes mockVolumes = Mockito.mock(Books.Volumes.class);
        Books.Volumes.List mockList = Mockito.mock(Books.Volumes.List.class);
        Books.Volumes.Get mockGet = Mockito.mock(Books.Volumes.Get.class);

        Volume mockVolume = new Volume();
        mockVolume.setId("OfywzQEACAAJ");
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setTitle("Test Book");
        mockVolume.setVolumeInfo(volumeInfo);

        Volumes mockVolumesResult = new Volumes();
        mockVolumesResult.setItems(Collections.singletonList(mockVolume));

        Mockito.when(mockBooks.volumes()).thenReturn(mockVolumes);
        Mockito.when(mockVolumes.list("isbn:9780743273565")).thenReturn(mockList);
        Mockito.when(mockList.execute()).thenReturn(mockVolumesResult);
        Mockito.when(mockVolumes.get("OfywzQEACAAJ")).thenReturn(mockGet);
        Mockito.when(mockGet.execute()).thenReturn(mockVolume);

        GoogleBooksHandler sut = new GoogleBooksHandler(mockBooks);
        Optional<String> citation =
            sut.processCitationParams("https://books.google.ro/books?vid=ISBN:9780743273565&hl=ro");

        Assert.assertTrue("Citation should be present", citation.isPresent());
        Assert.assertTrue("Citation should contain book title", citation.get().contains("title=Test Book"));
    }

    @Test
    public void testGetGoogleBooksTitleByEditionUrl() throws Exception {
        Books mockBooks = Mockito.mock(Books.class);
        Books.Volumes mockVolumes = Mockito.mock(Books.Volumes.class);
        Books.Volumes.Get mockGet = Mockito.mock(Books.Volumes.Get.class);

        Volume mockVolume = new Volume();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setTitle("Test Book");
        mockVolume.setVolumeInfo(volumeInfo);

        Mockito.when(mockBooks.volumes()).thenReturn(mockVolumes);
        Mockito.when(mockVolumes.get("OfywzQEACAAJ")).thenReturn(mockGet);
        Mockito.when(mockGet.execute()).thenReturn(mockVolume);

        GoogleBooksHandler sut = new GoogleBooksHandler(mockBooks);
        Optional<String> citation =
            sut.processCitationParams("https://books.google.ro/books/edition/Test_Book/OfywzQEACAAJ?hl=ro");

        Assert.assertTrue("Citation should be present", citation.isPresent());
        Assert.assertTrue("Citation should contain book title", citation.get().contains("title=Test Book"));
    }

}
