Set-Location D:\xiangmu2\01wuliupeisong\web
Start-Process -FilePath "node" -ArgumentList "node_modules\vite\bin\vite.js","--port","5173","--strictPort","--host" -WindowStyle Hidden -RedirectStandardOutput "D:\xiangmu2\01wuliupeisong\tools\vite.log" -RedirectStandardError "D:\xiangmu2\01wuliupeisong\tools\vite.err.log"
Write-Host "vite launched"
