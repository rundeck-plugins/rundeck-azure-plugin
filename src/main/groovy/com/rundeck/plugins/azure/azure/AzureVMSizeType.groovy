package com.rundeck.plugins.azure.azure

import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;

import java.util.List;

/**
 * Created by luistoledo on 11/17/17.
 */
public class AzureVMSizeType {
    public static final List<String> VM_SIZE_TYPE = [
            "Basic_A0",
            "Basic_A1",
            "Basic_A2",
            "Basic_A3",
            "Basic_A4",
            "Standard_A0",
            "Standard_A1",
            "Standard_A2",
            "Standard_A3",
            "Standard_A4",
            "Standard_DS1",
            "Standard_DS2",
            "Standard_DS3",
            "Standard_DS4",
            "Standard_DS11",
            "Standard_DS12",
            "Standard_DS13",
            "Standard_DS14",
            "Standard_A5",
            "Standard_A6",
            "Standard_A7",
            "Standard_D1_v2",
            "Standard_D2_v2",
            "Standard_D3_v2",
            "Standard_D4_v2",
            "Standard_D5_v2",
            "Standard_D11_v2",
            "Standard_D12_v2",
            "Standard_D13_v2",
            "Standard_D14_v2",
            "Standard_D15_v2",
            "Standard_D2_v2_Promo",
            "Standard_D3_v2_Promo",
            "Standard_D4_v2_Promo",
            "Standard_D5_v2_Promo",
            "Standard_D11_v2_Promo",
            "Standard_D12_v2_Promo",
            "Standard_D13_v2_Promo",
            "Standard_D14_v2_Promo",
            "Standard_F1",
            "Standard_F2",
            "Standard_F4",
            "Standard_F8",
            "Standard_F16",
            "Standard_A1_v2",
            "Standard_A2m_v2",
            "Standard_A2_v2",
            "Standard_A4m_v2",
            "Standard_A4_v2",
            "Standard_A8m_v2",
            "Standard_A8_v2",
            "Standard_D1",
            "Standard_D2",
            "Standard_D3",
            "Standard_D4",
            "Standard_D11",
            "Standard_D12",
            "Standard_D13",
            "Standard_D14",
            "Standard_DS1_v2",
            "Standard_DS2_v2",
            "Standard_DS3_v2",
            "Standard_DS4_v2",
            "Standard_DS5_v2",
            "Standard_DS11_v2",
            "Standard_DS12_v2",
            "Standard_DS13-2_v2",
            "Standard_DS13-4_v2",
            "Standard_DS13_v2",
            "Standard_DS14-4_v2",
            "Standard_DS14-8_v2",
            "Standard_DS14_v2",
            "Standard_DS15_v2",
            "Standard_DS2_v2_Promo",
            "Standard_DS3_v2_Promo",
            "Standard_DS4_v2_Promo",
            "Standard_DS5_v2_Promo",
            "Standard_DS11_v2_Promo",
            "Standard_DS12_v2_Promo",
            "Standard_DS13_v2_Promo",
            "Standard_DS14_v2_Promo",
            "Standard_F1s",
            "Standard_F2s",
            "Standard_F4s",
            "Standard_F8s",
            "Standard_F16s",
            "Standard_B1ms",
            "Standard_B1s",
            "Standard_B2ms",
            "Standard_B2s",
            "Standard_B4ms",
            "Standard_B8ms",
            "Standard_D2_v3",
            "Standard_D4_v3",
            "Standard_D8_v3",
            "Standard_D16_v3",
            "Standard_D32_v3",
            "Standard_D64_v3",
            "Standard_D2s_v3",
            "Standard_D4s_v3",
            "Standard_D8s_v3",
            "Standard_D16s_v3",
            "Standard_D32s_v3",
            "Standard_D64s_v3",
            "Standard_E2_v3",
            "Standard_E4_v3",
            "Standard_E8_v3",
            "Standard_E16_v3",
            "Standard_E32_v3",
            "Standard_E64_v3",
            "Standard_E2s_v3",
            "Standard_E4s_v3",
            "Standard_E8s_v3",
            "Standard_E16s_v3",
            "Standard_E32-8s_v3",
            "Standard_E32-16s_v3",
            "Standard_E32s_v3",
            "Standard_E64-16s_v3",
            "Standard_E64-32s_v3",
            "Standard_E64s_v3"
        ]

    String value

    AzureVMSizeType(String value) {
        this.value = value
    }

    VirtualMachineSizeTypes getImageSize(){
        return new VirtualMachineSizeTypes(this.value)

    }


    @Override
    public String toString() {
        return "AzureVMSizeType{" +
                "value='" + value + '\'' +
                '}';
    }
}
