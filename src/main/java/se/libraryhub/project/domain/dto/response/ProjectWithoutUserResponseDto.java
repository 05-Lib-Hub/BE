package se.libraryhub.project.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.libraryhub.favorite.domain.dto.FavoriteResponseDto;
import se.libraryhub.project.domain.Project;
import se.libraryhub.user.domain.User;
import se.libraryhub.user.domain.dto.response.UserContentResponseDto;
import se.libraryhub.user.domain.dto.response.UserResponseDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Schema(description = "사용자가 없는 프로젝트 미리보기")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectWithoutUserResponseDto{

    private Long projectId;

    private String projectname;

    private String description;

    private List<String> projectHashtags;

    private Boolean isPublic;

    private FavoriteResponseDto favoriteResponseDto;

    private LocalDateTime createdDate;

    @Builder
    public ProjectWithoutUserResponseDto(Long projectId, String projectname,
                              List<String> projectHashtags, Boolean isPublic, String description,
                              FavoriteResponseDto favoriteResponseDto, LocalDateTime createdDate) {
        this.projectId = projectId;
        this.description = description;
        this.projectname = projectname;
        this.projectHashtags = projectHashtags;
        this.isPublic = isPublic;
        this.createdDate = createdDate;
        this.favoriteResponseDto = favoriteResponseDto;
    }

    public static ProjectResponseDto of(Project project, List<String> projectHashtags, boolean isFollowed,
                                        FavoriteResponseDto favoriteResponseDto){
        return ProjectResponseDto.builder()
                .projectId(project.getProjectId())
                .description(project.getDescription())
                .projectname(project.getProjectname())
                .projectHashtags(projectHashtags)
                .favoriteResponseDto(favoriteResponseDto)
                .createdDate(project.getCreateDate())
                .isPublic(project.getIsPublic())
                .build();
    }


    public static List<ProjectResponseDto> sortByFavorite(List<ProjectResponseDto> projectResponseDtos) {
        List<ProjectResponseDto> sortedList = new ArrayList<>(projectResponseDtos);
        sortedList.sort(Comparator.comparingInt((ProjectResponseDto projectResponseDto) ->
                projectResponseDto.getFavoriteResponseDto().getFavoriteCount()).reversed());
        return sortedList;
    }

    public static List<ProjectResponseDto> sortByCreatedDateDesc(List<ProjectResponseDto> projectResponseDtos) {
        List<ProjectResponseDto> sortedList = new ArrayList<>(projectResponseDtos);
        sortedList.sort(Comparator.comparing(ProjectResponseDto::getCreatedDate).reversed());
        return sortedList;
    }
}
