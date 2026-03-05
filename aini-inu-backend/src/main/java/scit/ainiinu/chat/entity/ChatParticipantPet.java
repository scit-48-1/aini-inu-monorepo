package scit.ainiinu.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_participant_pet",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_participant_pet",
                columnNames = {"chat_participant_id", "pet_id"}
        )
)
public class ChatParticipantPet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_participant_id", nullable = false)
    private Long chatParticipantId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    private ChatParticipantPet(Long chatParticipantId, Long petId) {
        this.chatParticipantId = chatParticipantId;
        this.petId = petId;
    }

    public static ChatParticipantPet of(Long chatParticipantId, Long petId) {
        return new ChatParticipantPet(chatParticipantId, petId);
    }
}
