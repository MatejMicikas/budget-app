package cz.cvut.fit.budget_app.dto;

import cz.cvut.fit.budget_app.dto.request.CreateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateFundingSourceRequest;
import cz.cvut.fit.budget_app.entity.FundingSource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures {@code allocatedAmount} is truly optional (null) while remaining {@code @Positive} when set.
 */
class FundingSourceRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void create_nullAllocatedAmount_isValid() {
        CreateFundingSourceRequest r = baseCreate();
        r.setAllocatedAmount(null);
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void create_positiveAllocatedAmount_isValid() {
        CreateFundingSourceRequest r = baseCreate();
        r.setAllocatedAmount(new BigDecimal("15000.50"));
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void create_zeroAllocatedAmount_isInvalid() {
        CreateFundingSourceRequest r = baseCreate();
        r.setAllocatedAmount(BigDecimal.ZERO);
        assertThat(validator.validate(r).stream().map(ConstraintViolation::getPropertyPath).map(Object::toString))
                .contains("allocatedAmount");
    }

    @Test
    void update_nullAllocatedAmount_isValid() {
        UpdateFundingSourceRequest r = baseUpdate();
        r.setAllocatedAmount(null);
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void update_negativeAllocatedAmount_isInvalid() {
        UpdateFundingSourceRequest r = baseUpdate();
        r.setAllocatedAmount(new BigDecimal("-1"));
        assertThat(validator.validate(r)).isNotEmpty();
    }

    private CreateFundingSourceRequest baseCreate() {
        CreateFundingSourceRequest r = new CreateFundingSourceRequest();
        r.setName("Grant");
        r.setType(FundingSource.FundingType.PUBLIC_GRANT);
        r.setSeasonId(1L);
        return r;
    }

    private UpdateFundingSourceRequest baseUpdate() {
        UpdateFundingSourceRequest r = new UpdateFundingSourceRequest();
        r.setName("Grant");
        r.setType(FundingSource.FundingType.PUBLIC_GRANT);
        return r;
    }
}
