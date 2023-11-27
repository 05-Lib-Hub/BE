package se.libraryhub.favorite.service;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.libraryhub.favorite.domain.Favorite;
import se.libraryhub.favorite.domain.dto.FavoriteResponseDto;
import se.libraryhub.favorite.repository.FavoriteRepository;
import se.libraryhub.hashtag.domain.Hashtag;
import se.libraryhub.hashtag.repository.HashtagRepository;
import se.libraryhub.project.domain.Project;
import se.libraryhub.project.domain.dto.response.ProjectContentResponseDto;
import se.libraryhub.project.domain.dto.response.ProjectResponseDto;
import se.libraryhub.user.domain.Role;
import se.libraryhub.user.domain.User;

import java.util.List;
import java.util.stream.Collectors;

import static se.libraryhub.security.oauth.SecurityUtil.getCurrentUser;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final HashtagRepository hashtagRepository;

    public FavoriteResponseDto projectFavoriteInfo(Project project) {
        int favoriteCount = favoriteRepository.findAllByProject(project).size();
        boolean isLiked = favoriteRepository.existsByUserAndProject(getCurrentUser(), project);
        return FavoriteResponseDto.builder()
                .favoriteCount(favoriteCount)
                .isLiked(isLiked)
                .build();
    }

    public List<ProjectResponseDto> userFavoriteInfo() {
        List<Favorite> favoriteList = favoriteRepository.findAllByUser(getCurrentUser());
        return favoriteList.stream().map(favorite -> {
            Project favoriteProject = favorite.getProject();
            List<Hashtag> hashtags = hashtagRepository.findAllByProject(favoriteProject);
            List<String> hashtagContents = hashtags.stream().map(Hashtag::getContent).toList();
            return ProjectResponseDto.of(favoriteProject, hashtagContents, favoriteProject.getUser(),projectFavoriteInfo(favoriteProject));
        }).toList();
    }

    public void pressFavorite(Project project) {
        Favorite findFavorite = favoriteRepository.findByUserAndProject(getCurrentUser(), project).orElse(null);
        if (findFavorite != null) {
            favoriteRepository.delete(findFavorite);
            return;
        }
        favoriteRepository.save(Favorite.builder()
                .project(project)
                .user(getCurrentUser())
                .build());
    }
}
