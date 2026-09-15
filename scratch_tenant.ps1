$BasePath = "g:\Proyecto Taller\TallerMecanico_Backend\src\main\java\com\taller\backend\modules\talleres\model"
$entities = @("Cliente.java", "Vehiculo.java", "OrdenTrabajo.java", "Repuesto.java", "TipoServicio.java", "FormaPago.java", "Usuario.java")

foreach ($file in $entities) {
    if (Test-Path "$BasePath\$file") {
        $c = Get-Content "$BasePath\$file"
        $modified = $false
        
        # Check if TenantId is already there
        $hasTenantId = $false
        foreach ($line in $c) {
            if ($line -match "@TenantId") { $hasTenantId = $true }
        }
        
        if (-not $hasTenantId) {
            for ($i=0; $i -lt $c.Length; $i++) {
                if ($c[$i] -match "public class ") {
                    # Inject import org.hibernate.annotations.TenantId; at the top
                    for ($j=0; $j -lt $i; $j++) {
                        if ($c[$j] -match "^package ") {
                            $c = $c[0..$j] + "import org.hibernate.annotations.TenantId;" + $c[($j+1)..($c.Length-1)]
                            $i++
                            break
                        }
                    }
                    
                    # Inject the field right after the class declaration
                    $field = "    @TenantId`n    @Column(name = `"taller_id`")`n    private String tallerId;"
                    
                    # Some entities might already have a relationship with Taller, e.g. Taller taller.
                    # We should replace `@ManyToOne` Taller with `@TenantId` string, or keep `@ManyToOne` and annotate it with `@TenantId`.
                    # Let's annotate the existing field if it exists, otherwise add the string.
                    $foundTallerField = $false
                    for ($k=$i; $k -lt $c.Length; $k++) {
                        if ($c[$k] -match "private Taller taller;") {
                            $c[$k] = "    @TenantId`n" + $c[$k]
                            $foundTallerField = $true
                            break
                        }
                    }
                    
                    if (-not $foundTallerField) {
                        $c = $c[0..$i] + $field + $c[($i+1)..($c.Length-1)]
                    }
                    $modified = $true
                    break
                }
            }
        }
        
        if ($modified) {
            Set-Content -Path "$BasePath\$file" -Value ($c -join "`n")
        }
    }
}
