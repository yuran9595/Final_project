package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponseDTO {

        private boolean result;
        private String error;

        public ApiResponseDTO(boolean result) {
                this.result = result;
        }
}
