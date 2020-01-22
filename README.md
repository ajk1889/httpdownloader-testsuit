# httpdownloader-testsuit
A test server for HTTP Downloader

## Usage
1. Start server using `java -jar TestSuit.jar`
2. Type one of the available commands and press Enter to tweek Server parameters
3. Go to browser & type `localhost:1234`

## Available commands
#### htdocs=[/full/path/to/folder]
If htdocs is set, any request to `localhost:1234` will display contents of that folder. Enter `htdocs=` as input to disable this.
#### path123=[path/to/123/file]
If path123 is set, any request to that file will download 123 file. Even if `htdocs` is set & such a file already exists in it. 
By default `path123` is set to `/a.txt`. `path123` is mandatory & cannot be disabled. Change to different path if it conflicts with existing file
#### ping=[number]
Number of milliseconds, server should delay the response
#### sleep=[number]
Number of milliseconds server should sleep between writing each buffer
#### bfrsize=[number]
The number of bytes each buffer should hold
#### cookie=[string]
Server will respond with error 403 if cookie is not sent along with request. 
The response will always contain `Set-Cookie` header if this is set.
Enter `cookie=` as input to disable cookie verification
