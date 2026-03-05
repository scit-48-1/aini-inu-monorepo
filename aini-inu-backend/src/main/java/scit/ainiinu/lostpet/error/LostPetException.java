package scit.ainiinu.lostpet.error;

import scit.ainiinu.common.exception.BusinessException;

public class LostPetException extends BusinessException {

    public LostPetException(LostPetErrorCode errorCode) {
        super(errorCode);
    }
}
