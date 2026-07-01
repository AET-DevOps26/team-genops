package com.jobready.application.service;

import com.jobready.application.exception.ApplicationNotFoundException;
import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.generated.modelDto.CreateApplicationRequest;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.generated.modelDto.UpdateApplicationRequest;
import com.jobready.application.modelEntity.Application;
import com.jobready.application.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository repository;

    @InjectMocks
    private ApplicationServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void create_setsOwnerFromArgument_andDefaultsToApplied() {
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateApplicationRequest request = new CreateApplicationRequest("Acme", "Engineer")
            .jobDescription("Build things");

        JobApplication result = service.create(userId, request);

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getStage()).isEqualTo(ApplicationStage.APPLIED);
        assertThat(result.getCompany()).isEqualTo("Acme");
    }

    @Test
    void list_isScopedToTheUser() {
        Application a = entity(userId, ApplicationStage.APPLIED);
        when(repository.findByUserIdOrderByAppliedAtDesc(userId)).thenReturn(List.of(a));

        List<JobApplication> result = service.list(userId);

        assertThat(result).hasSize(1);
        verify(repository).findByUserIdOrderByAppliedAtDesc(userId);
    }

    @Test
    void get_whenNotOwned_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, id))
            .isInstanceOf(ApplicationNotFoundException.class);
    }

    @Test
    void update_changesStage() {
        UUID id = UUID.randomUUID();
        Application existing = entity(userId, ApplicationStage.APPLIED);
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicationRequest request =
            new UpdateApplicationRequest("Acme", "Engineer", ApplicationStage.INTERVIEW);

        JobApplication result = service.update(userId, id, request);

        assertThat(result.getStage()).isEqualTo(ApplicationStage.INTERVIEW);
        assertThat(existing.getStage()).isEqualTo(ApplicationStage.INTERVIEW);
    }

    @Test
    void delete_whenNotOwned_throwsAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, id))
            .isInstanceOf(ApplicationNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    private Application entity(UUID owner, ApplicationStage stage) {
        Application a = new Application();
        a.setId(UUID.randomUUID());
        a.setUserId(owner);
        a.setCompany("Acme");
        a.setJobTitle("Engineer");
        a.setStage(stage);
        return a;
    }
}
