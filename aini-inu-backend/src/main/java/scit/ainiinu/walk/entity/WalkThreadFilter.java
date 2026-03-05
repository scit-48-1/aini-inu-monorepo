package scit.ainiinu.walk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "thread_filter")
public class WalkThreadFilter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "values_json", nullable = false, length = 2000)
    private String valuesJson;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    private WalkThreadFilter(Long threadId, String type, String valuesJson, Boolean isRequired) {
        this.threadId = threadId;
        this.type = type;
        this.valuesJson = valuesJson;
        this.isRequired = isRequired;
    }

    public static WalkThreadFilter of(Long threadId, String type, String valuesJson, Boolean isRequired) {
        return new WalkThreadFilter(threadId, type, valuesJson, isRequired);
    }
}
