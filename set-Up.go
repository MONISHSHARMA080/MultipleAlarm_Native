package main

import (
	"archive/zip"
	"bufio"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
)

// CompressDir compresses the given directory into a zip file.
func CompressDir(dirPath string, outputZip string) error {
	// Create a new zip archive.
	zipFile, err := os.Create(outputZip)
	if err != nil {
		return err
	}
	defer zipFile.Close()

	archive := zip.NewWriter(zipFile)
	defer archive.Close()

	// Walk through the directory and add files to the zip archive.
	err = filepath.Walk(dirPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		// Skip directories, only compress files.
		if info.IsDir() {
			return nil
		}

		// Create a zip header for each file.
		relPath := strings.TrimPrefix(path, dirPath+string(filepath.Separator))
		header, err := zip.FileInfoHeader(info)
		if err != nil {
			return err
		}
		header.Name = relPath
		header.Method = zip.Deflate

		// Write the file to the zip archive.
		writer, err := archive.CreateHeader(header)
		if err != nil {
			return err
		}
		file, err := os.Open(path)
		if err != nil {
			return err
		}
		defer file.Close()

		_, err = io.Copy(writer, file)
		return err
	})

	return err
}

// Decompress extracts the contents of the zip file into the target directory.
func Decompress(zipPath string, outputDir string) error {
	zipReader, err := zip.OpenReader(zipPath)
	if err != nil {
		return err
	}
	defer zipReader.Close()

	for _, file := range zipReader.File {
		// Construct the output path.
		outputPath := filepath.Join(outputDir, file.Name)

		// If it's a directory, create it.
		if file.FileInfo().IsDir() {
			err := os.MkdirAll(outputPath, os.ModePerm)
			if err != nil {
				return err
			}
			continue
		}

		// Create the directory for the file.
		if err := os.MkdirAll(filepath.Dir(outputPath), os.ModePerm); err != nil {
			return err
		}

		// Create the file.
		outFile, err := os.OpenFile(outputPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, file.Mode())
		if err != nil {
			return err
		}
		defer outFile.Close()

		// Open the file inside the zip archive.
		inFile, err := file.Open()
		if err != nil {
			return err
		}
		defer inFile.Close()

		// Copy the content of the file.
		_, err = io.Copy(outFile, inFile)
		if err != nil {
			return err
		}
	}

	return nil
}

func main() {
	// Specify the directory to compress and the output zip file name.
	cli_arg := os.Args
	if len(cli_arg) != 2{
		println("Error: usage ./<fileName> zip or unzip")
		os.Exit(3)
	}
	if cli_arg[1] != "zip" && cli_arg[1] != "unzip"{
		println("Error: usage ./<fileName> zip or unzip")
		os.Exit(3)
	}


	dirToCompress := "/app/src/main/resc/raw"

	println("default dir for the .mp3 files is /app/src/main/res/raw ")
	println("do you want to change it (y/n)")
	reader := bufio.NewReader(os.Stdin)
	stringRead, err_form_read_str := reader.ReadString('\n')
	if err_form_read_str != nil {
		fmt.Printf("Error occurred while reading the input: %v\n", err_form_read_str)
		os.Exit(1)
	}

	// Trim the newline character from the input
	stringRead = strings.TrimSpace(stringRead)

	// Check if the input is not y, Y, n, or N
	if stringRead != "y" && stringRead != "Y" && stringRead != "n" && stringRead != "N" {
		fmt.Printf("Valid inputs are only y or n, you entered: %s\n", stringRead)
		os.Exit(1)
	}
	println("what you entered: "+stringRead)

	


	if stringRead == "Y" || stringRead == "y" {
		println("enter the dir name: ")
		dir_enterd_by_the_user, err_form_read_dir := reader.ReadString(' ')
		if err_form_read_dir != nil {
			fmt.Printf("Error occurred while reading the input: %v\n", err_form_read_dir)
			os.Exit(1)
		}
		dirToCompress = dir_enterd_by_the_user
	}

	zipFileName := "alarm_sound.zip"

	// Compress the directory.
	pwd, error_from_wd := os.Getwd()
	if error_from_wd != nil {
		println("error in getting cwd ", error_from_wd.Error())
		os.Exit(2)
	}
	dirToCompress = pwd + dirToCompress
	fmt.Print("Dir is ", dirToCompress, "\n")
	fmt.Println("Compressing directory..., dir chosen is ",dirToCompress)
	if cli_arg[1] == "zip"{
		err := CompressDir(dirToCompress, zipFileName)
		if err != nil {
			fmt.Println("Error compressing directory:", err)
			return
		}
		fmt.Println("Directory compressed successfully!")
	
	}
	
	if cli_arg[1] == "unzip"{
		outputDir := dirToCompress // to change
	fmt.Println("Decompressing zip file...", "\n file name %s and the output dir is ->%s",zipFileName, outputDir)

	err_from_decomp := Decompress(zipFileName, outputDir)
	if err_from_decomp != nil {
		fmt.Println("Error decompressing zip file:", err_from_decomp)
		return
	}
	fmt.Println("Zip file decompressed successfully!")
	
	}
	// Decompress the zip file into the specified output directory.
	
	return
}
