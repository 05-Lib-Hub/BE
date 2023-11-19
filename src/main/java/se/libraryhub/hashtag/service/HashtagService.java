package se.libraryhub.hashtag.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.libraryhub.hashtag.domain.Hashtag;
import se.libraryhub.hashtag.repository.HashtagRepository;
import se.libraryhub.library.domain.Library;
import se.libraryhub.library.service.LibraryService;
import se.libraryhub.project.domain.Project;
import se.libraryhub.project.service.ProjectService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HashtagService {

    private final HashtagRepository hashtagRepository;
    private final ProjectService projectService;
    private final LibraryService libraryService;

    public List<Hashtag> getProjectHashtags(Long projectId) {
        Project findProject = projectService.findProjectById(projectId);
        return hashtagRepository.findAllByProject(findProject);
    }

    public List<Hashtag> getLibraryHashtags(Long libraryId){
        Library findLibrary = libraryService.findLibraryByLibraryId(libraryId);
        return hashtagRepository.findAllByLibrary(findLibrary);
    }

    public List<Hashtag> searchHashtagsFromProject(Long projectId, String content){
        return getProjectHashtags(projectId).stream()
                .filter(hashtag -> hashtag.getContent().contains(content))
                .toList();
    }

    public List<Hashtag> searchHashtagsFromLibrary(Long libraryId, String content){
        return getLibraryHashtags(libraryId).stream()
                .filter(hashtag -> hashtag.getContent().contains(content))
                .toList();
    }
}