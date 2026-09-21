package br.com.acme.eligibility.infrastructure.toggle;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Contrato HTTP do servico externo de toggles. */
public interface ToggleApi {

    @GET("/toggles/{product}")
    Call<ToggleResponse> getToggle(@Path("product") String product,
                                   @Header("X-Cnpj") String cnpj,
                                   @Query("regiao") String regiao,
                                   @Query("dicom") String dicom);
}
