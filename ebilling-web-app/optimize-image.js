/**
 * Script para optimizar la imagen de fondo del login
 * 
 * Uso:
 * 1. npm install sharp --save-dev
 * 2. node optimize-image.js
 */

const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const inputImage = path.join(__dirname, 'src/assets/wallparisoft-bg.png');
const outputWebP = path.join(__dirname, 'src/assets/wallparisoft-bg.webp');
const outputOptimizedPNG = path.join(__dirname, 'src/assets/wallparisoft-bg-optimized.png');

async function optimizeImage() {
  try {
    console.log('🖼️  Optimizando imagen de fondo...');
    
    // Verificar que la imagen existe
    if (!fs.existsSync(inputImage)) {
      console.error('❌ No se encontró la imagen:', inputImage);
      return;
    }

    // Obtener tamaño original
    const originalStats = fs.statSync(inputImage);
    console.log(`📊 Tamaño original: ${(originalStats.size / 1024).toFixed(2)} KB`);

    // Crear versión WebP (mejor compresión)
    await sharp(inputImage)
      .webp({ quality: 80, effort: 6 })
      .toFile(outputWebP);
    
    const webpStats = fs.statSync(outputWebP);
    const webpReduction = ((1 - webpStats.size / originalStats.size) * 100).toFixed(1);
    console.log(`✅ WebP creado: ${(webpStats.size / 1024).toFixed(2)} KB (${webpReduction}% reducción)`);

    // Crear versión PNG optimizada
    await sharp(inputImage)
      .png({ compressionLevel: 9, quality: 85 })
      .toFile(outputOptimizedPNG);
    
    const pngStats = fs.statSync(outputOptimizedPNG);
    const pngReduction = ((1 - pngStats.size / originalStats.size) * 100).toFixed(1);
    console.log(`✅ PNG optimizado: ${(pngStats.size / 1024).toFixed(2)} KB (${pngReduction}% reducción)`);

    console.log('\n📝 Próximos pasos:');
    console.log('1. Revisa las imágenes generadas');
    console.log('2. Si WebP se ve bien, actualiza el CSS para usar wallparisoft-bg.webp');
    console.log('3. Si prefieres PNG, reemplaza wallparisoft-bg.png con wallparisoft-bg-optimized.png');
    
  } catch (error) {
    console.error('❌ Error al optimizar:', error.message);
    console.log('\n💡 Alternativa: Usa herramientas online como TinyPNG.com o Squoosh.app');
  }
}

optimizeImage();

