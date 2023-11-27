package se.libraryhub.project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.libraryhub.favorite.domain.dto.FavoriteResponseDto;
import se.libraryhub.favorite.service.FavoriteService;
import se.libraryhub.global.error.project.ProjectNotFoundException;
import se.libraryhub.global.error.project.ProjectPageException;
import se.libraryhub.hashtag.domain.Hashtag;
import se.libraryhub.hashtag.repository.HashtagRepository;
import se.libraryhub.library.domain.dto.response.LibraryContentResponseDto;
import se.libraryhub.library.service.LibraryService;
import se.libraryhub.project.domain.PagingMode;
import se.libraryhub.project.domain.Project;
import se.libraryhub.project.domain.dto.request.ProjectContentRequestDto;
import se.libraryhub.project.domain.dto.response.ProjectContentResponseDto;
import se.libraryhub.project.domain.dto.response.ProjectResponseDto;
import se.libraryhub.project.domain.dto.response.ProjectResult;
import se.libraryhub.project.repository.ProjectRepository;
import se.libraryhub.user.domain.User;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService{

    private static final int PAGE = 10;
    private final ProjectRepository projectRepository;
    private final HashtagRepository hashtagRepository;
    private final LibraryService libraryService;
    private final FavoriteService favoriteService;

    public ProjectResponseDto postProject(ProjectContentRequestDto projectContentRequestDto, User user) {
        Project project = ProjectContentRequestDto.toEntity(projectContentRequestDto, user);
        Project postedProject = projectRepository.save(project);

        projectContentRequestDto.getProjectHashtags().forEach(s -> {
            hashtagRepository.save(Hashtag.projectHashtag(postedProject, s));
        });
        List<String> projectHashtags = hashtagRepository.findAllByProject(postedProject).stream()
                .map(Hashtag::getContent).toList();
        FavoriteResponseDto favoriteResponseDto = favoriteService.projectFavoriteInfo(postedProject);
        return ProjectResponseDto.of(postedProject, projectHashtags, user, favoriteResponseDto);
    }

    public ProjectResult getProjectList(){
        List<Project> publicProjects = projectRepository.findByIsPublicTrue();

        List<ProjectResponseDto> projectResponseDtos = publicProjects.stream().map((project -> {
            List<String> projectHashtags = hashtagRepository.findAllByProject(project).stream()
                    .map(Hashtag::getContent).toList();
            FavoriteResponseDto favoriteResponseDto = favoriteService.projectFavoriteInfo(project);
            return ProjectResponseDto.of(project, projectHashtags, project.getUser(), favoriteResponseDto);
        })).toList();

        int totalPage = projectResponseDtos.size() / PAGE;

        return new ProjectResult(projectResponseDtos, totalPage);
    }

    public ProjectResult getUserProjectList(User user){
        List<Project> userProjects = projectRepository.findAllByUser(user);

        List<ProjectResponseDto> projectResponseDtos = userProjects.stream().map((project -> {
            List<String> projectHashtags = hashtagRepository.findAllByProject(project).stream()
                    .map(Hashtag::getContent).toList();
            FavoriteResponseDto favoriteResponseDto = favoriteService.projectFavoriteInfo(project);
            return ProjectResponseDto.of(project, projectHashtags, user, favoriteResponseDto);
        })).toList();

        int totalPage = projectResponseDtos.size() / PAGE;

        return new ProjectResult(projectResponseDtos, totalPage);
    }

    public ProjectContentResponseDto getProject(Long projectId){
        Project project = projectRepository.findProjectByProjectId(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        List<String> projectHashtags = hashtagRepository.findAllByProject(project).stream().map(Hashtag::getContent).toList();
        List<LibraryContentResponseDto> libraryContentResponseDtos = libraryService.getProjectLibraries(project);
        FavoriteResponseDto favoriteResponseDto = favoriteService.projectFavoriteInfo(project);
        return ProjectContentResponseDto.of(project, projectHashtags, libraryContentResponseDtos, favoriteResponseDto);
    }

    public ProjectContentResponseDto updateProject(Long projectId, ProjectContentRequestDto projectContentRequestDto){
        Project findProject = projectRepository.findProjectByProjectId(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        Project.updateProject(findProject, projectContentRequestDto);
        Project updatedProject = projectRepository.save(findProject);

        hashtagRepository.deleteAllByProject(updatedProject);
        projectContentRequestDto.getProjectHashtags().forEach(s -> {
            hashtagRepository.save(Hashtag.projectHashtag(updatedProject, s));
        });
        List<String> projectHashtags = hashtagRepository.findAllByProject(updatedProject).stream().map(
                Hashtag::getContent
        ).toList();

        List<LibraryContentResponseDto> libraryContentResponseDtos = libraryService.getProjectLibraries(updatedProject);

        FavoriteResponseDto favoriteResponseDto = favoriteService.projectFavoriteInfo(updatedProject);

        return ProjectContentResponseDto.of(updatedProject, projectHashtags, libraryContentResponseDtos, favoriteResponseDto);
    }

    public void deleteProject(Long projectId){
        Project findProject = projectRepository.findProjectByProjectId(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        projectRepository.delete(findProject);
    }

    public ProjectResult pagingProjects(int pageNumber, PagingMode pagingMode){
        ProjectResult projectList = getProjectList();
        return pagingProjectsWithMode(projectList.getProjectResult(), pageNumber, pagingMode);
    }

    public ProjectResult pagingMyProjects(User user, int pageNumber, PagingMode pagingMode) {
        ProjectResult projectList = getUserProjectList(user);
        return pagingProjectsWithMode(projectList.getProjectResult(), pageNumber, pagingMode);
    }

    public void pressFavorite(Long projectId) {
        favoriteService.pressFavorite(projectRepository.findProjectByProjectId(projectId)
                .orElseThrow(ProjectNotFoundException::new));
    }

    private ProjectResult pagingProjectsWithMode(List<ProjectResponseDto> projectResponseDtos, int pageNumber, PagingMode pagingMode){
        List<ProjectResponseDto> sortedDto = new ArrayList<>();
        if(pagingMode.equals(PagingMode.LATEST)){
            sortedDto = ProjectResponseDto.sortByCreatedDateDesc(projectResponseDtos);
        }
        if(pagingMode.equals(PagingMode.POPULAR)){
            sortedDto = ProjectResponseDto.sortByFavorite(projectResponseDtos);
        }
        return getPage(sortedDto, pageNumber, PAGE);
    }

    public static ProjectResult getPage(List<ProjectResponseDto> projectResponseDtos, int pageNumber, int pageSize) {
        int totalItems = projectResponseDtos.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        if (pageNumber < 1 || pageNumber > totalPages) {
            throw new ProjectPageException();
        }

        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        return ProjectResult.builder()
                .projectResult(projectResponseDtos.subList(startIndex, endIndex))
                .totalPage(totalPages)
                .build();
    }
}
