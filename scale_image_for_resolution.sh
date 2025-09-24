#!/bin/zsh
#---------------------------------------------------------------
# Given an xxhdpi image or an composeApp Icon (launcher), this script
# creates different dpis resources
#
# Place this script, as well as the source image, inside project
# folder and execute it passing the image filename as argument
#
# Example:
# ./scale_image_for_resolution.sh ic_launcher.png
# OR
# ./scale_image_for_resolution.sh my_cool_xxhdpi_image.png
#---------------------------------------------------------------

echo " Creating different dimensions (dips) of "$1" ..."

if [ $1 = "ic_launcher.png" ]; then
    echo "  App icon detected"

    magick $1 -resize 192x192 composeApp/src/androidMain/res/mipmap-xxxhdpi/$1
    magick $1 -resize 144x144 composeApp/src/androidMain/res/mipmap-xxhdpi/$1
    magick $1 -resize 96x96 composeApp/src/androidMain/res/mipmap-xhdpi/$1
    magick $1 -resize 72x72 composeApp/src/androidMain/res/mipmap-hdpi/$1
    magick $1 -resize 48x48 composeApp/src/androidMain/res/mipmap-mdpi/$1
    rm -i $1
elif [ $1 = "ic_launcher_round.png" ]; then
    echo "  App round icon detected"

    magick $1 -resize 192x192 composeApp/src/androidMain/res/mipmap-xxxhdpi/$1
    magick $1 -resize 144x144 composeApp/src/androidMain/res/mipmap-xxhdpi/$1
    magick $1 -resize 96x96 composeApp/src/androidMain/res/mipmap-xhdpi/$1
    magick $1 -resize 72x72 composeApp/src/androidMain/res/mipmap-hdpi/$1
    magick $1 -resize 48x48 composeApp/src/androidMain/res/mipmap-mdpi/$1
    rm -i $1
else
    mkdir -p composeApp/src/commonMain/composeResources/{drawable-xhdpi,drawable-hdpi,drawable-mdpi,drawable-xxhdpi}
    magick $1 -resize 67% composeApp/src/commonMain/composeResources/drawable-xhdpi/$1
    magick $1 -resize 50% composeApp/src/commonMain/composeResources/drawable-hdpi/$1
    magick $1 -resize 33% composeApp/src/commonMain/composeResources/drawable-mdpi/$1
    mv $1 composeApp/src/commonMain/composeResources/drawable-xxhdpi/$1

fi

echo " Done"
