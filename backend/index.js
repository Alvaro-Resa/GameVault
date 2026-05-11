require('dotenv').config();

const express = require('express');
const mysql = require('mysql2');
const bcrypt = require('bcrypt');
const path = require('path');
const jwt = require('jsonwebtoken');
const nodemailer = require('nodemailer'); 
const saltRounds = 10;
const app = express();

app.use(express.json());
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));
// Sirviendo la carpeta exterior imgBBDD directamente
app.use('/imgBBDD', express.static(path.join(__dirname, '../imgBBDD')));

const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME
});

db.connect(err => { 
    if (err) throw err;
    console.log('¡Conectado a la base de datos!');
});

// Configuramos el transportador de correo para Gmail
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS
    }
});

const sanitizeInput = (str) => {
    return str.replace(/[^a-zA-Z0-9_\-\*\@\.]/g, '');
};

const verifyToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) return res.status(401).json({ message: "Access denied. No token provided." });

    jwt.verify(token, process.env.JWT_SECRET, (err, decoded) => {
        if (err) return res.status(403).json({ message: "Invalid or expired token." });
        req.user = decoded;
        next();
    });
};

// MÓDULO 1: GESTIÓN DE JUEGOS
app.get('/api/games', (req, res) => {
    const { sort } = req.query; 
    let orderBy = "g.title ASC"; 

    if (sort === 'rating') orderBy = "g.average_rating DESC";
    if (sort === 'date') orderBy = "g.release_date DESC";

    const sql = `
        SELECT g.*, d.name as developer_name, 
        (SELECT GROUP_CONCAT(gn.name SEPARATOR ', ') 
         FROM game_genres gg 
         JOIN genres gn ON gg.genre_id = gn.id 
         WHERE gg.game_id = g.id) as genre_name
        FROM games g 
        LEFT JOIN developers d ON g.developer_id = d.id
        ORDER BY ${orderBy}`;

    db.query(sql, (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(results);
    });
});

app.delete('/api/games/:id', verifyToken, (req, res) => {
    db.query("DELETE FROM Games WHERE id = ?", [req.params.id], (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ message: "Game deleted successfully" });
    });
});

app.get('/api/games/:id', (req, res) => {
    const sql = `
        SELECT g.*, d.name as developer_name, 
        (SELECT GROUP_CONCAT(gn.name SEPARATOR ', ') 
         FROM Game_Genres gg 
         JOIN Genres gn ON gg.genre_id = gn.id 
         WHERE gg.game_id = g.id) as genre_name
        FROM Games g 
        LEFT JOIN Developers d ON g.developer_id = d.id
        WHERE g.id = ?`;

    db.query(sql, [req.params.id], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        if (results.length === 0) return res.status(404).json({ message: "Game not found" });
        const game = results[0];
        game.image_url = game.image ? `http://10.0.2.2:3000/imgBBDD/${game.image}` : null;
        res.json(game);
    });
});

// MÓDULO 2: AUTENTICACIÓN Y USUARIO
app.post('/api/users/register', async (req, res) => {
    const { username, nickname, email, password } = req.body;
    try {
        const hashedPassword = await bcrypt.hash(password, saltRounds);
        const sql = "INSERT INTO Users (username, nickname, email, password, role, state) VALUES (?, ?, ?, ?, 'user', 1)";
        
        db.query(sql, [username, nickname, email, hashedPassword], (err, result) => {
            if (err) {
                if (err.code === 'ER_DUP_ENTRY') return res.status(409).json({ message: "Username or email already exists" });
                return res.status(500).json({ error: err.message });
            }
            res.status(201).json({ message: "Account created successfully" });
        });
    } catch (error) {
        res.status(500).json({ error: "Error encrypting password" });
    }
});

app.post('/api/users/login', (req, res) => {
    let { username, password } = req.body;
    username = sanitizeInput(username);

    const sql = "SELECT * FROM Users WHERE (username = ? OR email = ?) AND state = 1";

    db.query(sql, [username, username], async (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        if (results.length === 0) return res.status(401).json({ message: "User not found or banned" });

        const user = results[0];

        try {
            const match = await bcrypt.compare(password, user.password);
            if (match) {
                const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
                const expiresAt = new Date();
                expiresAt.setMinutes(expiresAt.getMinutes() + 5);

                const updateSql = "UPDATE Users SET otp_code = ?, otp_expires_at = ? WHERE id = ?";
                db.query(updateSql, [otpCode, expiresAt, user.id], (updateErr) => {
                    if (updateErr) return res.status(500).json({ error: updateErr.message });
                    
                    const mailOptions = {
                        from: process.env.EMAIL_USER,
                        to: user.email,
                        subject: 'GameVault - Your Verification Code',
                        html: `
                            <div style="font-family: Arial, sans-serif; text-align: center; padding: 30px; background-color: #f4f4f4;">
                                <div style="max-width: 500px; margin: auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0px 4px 10px rgba(0,0,0,0.1);">
                                    <h2 style="color: #333;">Welcome back to GameVault!</h2>
                                    <p style="color: #666; font-size: 16px;">Here is your secure verification code:</p>
                                    <h1 style="color: #4CAF50; font-size: 48px; letter-spacing: 8px; margin: 20px 0;">${otpCode}</h1>
                                    <p style="color: #999; font-size: 14px;">This code will expire in 5 minutes. Do not share it with anyone.</p>
                                </div>
                            </div>
                        `
                    };

                    transporter.sendMail(mailOptions, (mailErr, info) => {
                        if (mailErr) {
                            console.error("Error SMTP al enviar correo:", mailErr);
                            return res.status(500).json({ error: `SMTP Error: ${mailErr.message}` });
                        }
                        res.status(200).json({ message: "OTP sent", userId: user.id });
                    });
                });
            } else {
                res.status(401).json({ message: "Incorrect password" });
            }
        } catch (compareErr) {
            res.status(500).json({ error: "Internal error checking credentials" });
        }
    });
});

app.post('/api/users/verify-otp', (req, res) => {
    const { userId, otpCode } = req.body;
    const sql = "SELECT * FROM Users WHERE id = ? AND state = 1";
    
    db.query(sql, [userId], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        if (results.length === 0) return res.status(404).json({ message: "User not found" });

        const user = results[0];

        if (!user.otp_code) return res.status(400).json({ success: false, message: "No pending OTP code" });
        if (user.otp_code !== otpCode) return res.status(401).json({ success: false, message: "Incorrect OTP code" });
        
        if (new Date() > new Date(user.otp_expires_at)) {
            return res.status(401).json({ success: false, message: "Expired OTP code" });
        }

        db.query("UPDATE Users SET otp_code = NULL, otp_expires_at = NULL WHERE id = ?", [userId], () => {});

        const token = jwt.sign({ id: user.id }, process.env.JWT_SECRET, { expiresIn: '30d' });

        res.status(200).json({ success: true, message: "Verification successful", token: token });
    });
});

app.get('/api/users', verifyToken, (req, res) => {
    db.query("SELECT id, username, email, role, state FROM Users", (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(results);
    });
});

app.put('/api/users/:id/ban', verifyToken, (req, res) => {
    db.query("SELECT state FROM Users WHERE id = ?", [req.params.id], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        if (results.length === 0) return res.status(404).json({ message: "User not found" });
        
        const newState = results[0].state === '1' || results[0].state === 1 ? 0 : 1;
        db.query("UPDATE Users SET state = ? WHERE id = ?", [newState, req.params.id], (upErr) => {
            if (upErr) return res.status(500).json({ error: upErr.message });
            res.json({ message: newState === 1 ? "User unbanned" : "User banned", state: newState });
        });
    });
});

app.get('/api/users/:id', verifyToken, (req, res) => {
    const sql = "SELECT id, username, nickname, email, role, state, bio, avatar_img FROM Users WHERE id = ?";
    db.query(sql, [req.params.id], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        if (results.length === 0) return res.status(404).json({ message: "Usuario no encontrado" });
        res.json(results[0]);
    });
});

app.patch('/api/users/:id', verifyToken, (req, res) => {
    const { nickname, bio, avatar_img } = req.body;
    const sql = "UPDATE Users SET nickname = ?, bio = ?, avatar_img = ? WHERE id = ?";
    db.query(sql, [nickname, bio, avatar_img, req.params.id], (err, result) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ message: "Perfil actualizado con éxito" });
    });
});

app.get('/api/users/:id/stats', verifyToken, (req, res) => {
    const userId = req.params.id;
    const sql = `
        SELECT 
            (SELECT COUNT(*) FROM games_library gl JOIN Library l ON gl.library_id = l.id WHERE l.user_id = ?) as total_games,
            (SELECT COUNT(*) FROM games_library gl JOIN Library l ON gl.library_id = l.id WHERE l.user_id = ? AND gl.status = 'completed') as completed_games,
            (SELECT COUNT(*) FROM Reviews WHERE user_id = ?) as total_reviews,
            (SELECT IFNULL(SUM(hours_played), 0) FROM games_library gl JOIN Library l ON gl.library_id = l.id WHERE l.user_id = ?) as total_hours
    `;
    db.query(sql, [userId, userId, userId, userId], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(results[0]);
    });
});

// MÓDULO 3: BIBLIOTECA (¡CON FIX DEL SERVER ERROR!)
app.get('/api/library/:userId', verifyToken, (req, res) => {
    const sql = `
        SELECT 
            g.id AS id, g.title, g.description, g.release_date, g.image, 
            g.developer_id, g.average_rating, gl.status, gl.hours_played,
            d.name as developer_name,
            (SELECT GROUP_CONCAT(gn.name SEPARATOR ', ') 
             FROM Game_Genres gg 
             JOIN Genres gn ON gg.genre_id = gn.id 
             WHERE gg.game_id = g.id) as genre_name
        FROM games_library gl
        JOIN Library l ON gl.library_id = l.id 
        JOIN Games g ON gl.game_id = g.id 
        LEFT JOIN Developers d ON g.developer_id = d.id
        WHERE l.user_id = ?`;

    db.query(sql, [req.params.userId], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        const processedResults = results.map(game => ({
            ...game,
            image_url: game.image ? `http://10.0.2.2:3000/imgBBDD/${game.image}` : null
        }));
        res.json(processedResults);
    });
});

app.post('/api/library', verifyToken, (req, res) => {
    const { user_id, game_id, status, hours_played } = req.body;

    // LOG 1: Ver qué datos están llegando desde Android
    console.log("--- INTENTO DE GUARDADO EN BIBLIOTECA ---");
    console.log("Datos recibidos:", { user_id, game_id, status, hours_played });

    if (!user_id || !game_id) {
        console.error("ERROR: Faltan IDs obligatorios");
        return res.status(400).json({ error: "user_id y game_id son obligatorios" });
    }

    // 1. Buscamos o creamos la cabecera en 'Library'
    db.query("SELECT id FROM Library WHERE user_id = ?", [user_id], (err, results) => {
        if (err) {
            console.error("ERROR SQL (Buscando Library):", err.sqlMessage || err);
            return res.status(500).json({ error: "Error en Library: " + err.message });
        }

        const proceedWithInsert = (libId) => {
            console.log("Usando Library ID:", libId);
            
            const checkSql = "SELECT * FROM games_library WHERE library_id = ? AND game_id = ?";
            db.query(checkSql, [libId, game_id], (checkErr, checkRes) => {
                if (checkErr) {
                    console.error("ERROR SQL (Verificando existencia):", checkErr.sqlMessage || checkErr);
                    return res.status(500).json({ error: checkErr.message });
                }

                if (checkRes.length > 0) {
                    console.log("El juego ya existe, actualizando...");
                    const updateSql = "UPDATE games_library SET status = ?, hours_played = ? WHERE library_id = ? AND game_id = ?";
                    db.query(updateSql, [status, hours_played || 0, libId, game_id], (upErr) => {
                        if (upErr) {
                            console.error("ERROR SQL (Update):", upErr.sqlMessage || upErr);
                            return res.status(500).json({ error: upErr.message });
                        }
                        res.json({ message: "Juego actualizado" });
                    });
                } else {
                    console.log("El juego es nuevo, insertando...");
                    const insertSql = "INSERT INTO games_library (library_id, game_id, status, hours_played) VALUES (?, ?, ?, ?)";
                    db.query(insertSql, [libId, game_id, status, hours_played || 0], (inErr) => {
                        if (inErr) {
                            console.error("ERROR SQL (Insert):", inErr.sqlMessage || inErr);
                            return res.status(500).json({ error: inErr.message });
                        }
                        res.status(201).json({ message: "Juego guardado" });
                    });
                }
            });
        };

        if (results.length === 0) {
            console.log("No existe Library para este usuario. Creando una...");
            db.query("INSERT INTO Library (user_id) VALUES (?)", [user_id], (errNew, resultNew) => {
                if (errNew) {
                    console.error("ERROR SQL (Creando Library):", errNew.sqlMessage || errNew);
                    return res.status(500).json({ error: "No se pudo crear Library: " + errNew.message });
                }
                proceedWithInsert(resultNew.insertId);
            });
        } else {
            proceedWithInsert(results[0].id);
        }
    });
});

app.put('/api/library/:userId/:gameId', verifyToken, (req, res) => {
    const { status, hours_played } = req.body;
    const sql = `
        UPDATE games_library gl
        JOIN Library l ON gl.library_id = l.id
        SET gl.status = ?, gl.hours_played = ?
        WHERE l.user_id = ? AND gl.game_id = ?`;
    db.query(sql, [status, hours_played, req.params.userId, req.params.gameId], (err, result) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ message: "Library status updated" });
    });
});

// MÓDULO 4: RESEÑAS
app.post('/api/reviews', verifyToken, (req, res) => {
    const { user_id, game_id, rating, title, content } = req.body; 
    const sql = "INSERT INTO Reviews (user_id, game_id, rating, title, content) VALUES (?, ?, ?, ?, ?)";
    db.query(sql, [user_id, game_id, rating, title, content], (err, result) => {
        if (err) return res.status(500).json({ error: err.message });
        res.status(201).json({ message: "Review added", reviewId: result.insertId });
    });
});

app.get('/api/games/:gameId/reviews', (req, res) => {
    db.query("SELECT * FROM Reviews WHERE game_id = ?", [req.params.gameId], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(results);
    });
});

app.put('/api/reviews/:id', verifyToken, (req, res) => {
    const { rating, title, content } = req.body;
    db.query("UPDATE Reviews SET rating = ?, title = ?, content = ? WHERE id = ?", [rating, title, content, req.params.id], (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ message: "Review updated" });
    });
});

app.delete('/api/reviews/:id', verifyToken, (req, res) => {
    db.query("DELETE FROM Reviews WHERE id = ?", [req.params.id], (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ message: "Review deleted" });
    });
});

app.get('/api/users/:userId/reviews', (req, res) => {
    db.query("SELECT * FROM Reviews WHERE user_id = ?", [req.params.userId], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(results);
    });
});

// MÓDULO 5: AUXILIARES E INFORMACIÓN (AQUÍ ESTÁ LA NUEVA RUTA DE DEVELOPERS)
app.get('/api/genres', (req, res) => {
    db.query("SELECT * FROM Genres", (err, results) => {
        if (err) return res.status(500).json(err);
        res.json(results);
    });
});

app.get('/api/developers', (req, res) => {
    db.query("SELECT * FROM Developers", (err, results) => {
        if (err) return res.status(500).json(err);
        res.json(results);
    });
});

app.get('/api/developers/:id', (req, res) => {
    const devId = req.params.id;
    db.query("SELECT * FROM developers WHERE id = ?", [devId], (err, devResults) => {
        if (err || devResults.length === 0) return res.status(404).json({ message: "Developer not found" });

        const developer = devResults[0];
        // Importante: Asegurar que la ruta de la imagen sea correcta para el adaptador de Android
        const sqlGames = `SELECT *, CONCAT('http://10.0.2.2:3000/imgBBDD/Games/', image) as image_url FROM games WHERE developer_id = ?`;
            
        db.query(sqlGames, [devId], (err, gameResults) => {
            if (err) return res.status(500).json({ error: err.message });
            res.json({ 
                id: developer.id, 
                name: developer.name, 
                works: gameResults 
            });
        });
    });
});

// MÓDULO 6: GESTIÓN DE MANGAS 
// Obtener todos los mangas (Corregido para el Menú Principal)
app.get('/api/mangas', (req, res) => {
    const { sort } = req.query;
    let orderBy = "m.title ASC";

    if (sort === 'rating') orderBy = "m.average_rating DESC";
    if (sort === 'date') orderBy = "m.release_date DESC";

    const sql = `
        SELECT m.*, p.name as publisher_name, 
        (SELECT GROUP_CONCAT(a.name SEPARATOR ', ') 
         FROM manga_authors ma 
         JOIN authors a ON ma.author_id = a.id 
         WHERE ma.manga_id = m.id) as author_names,
        (SELECT author_id FROM manga_authors WHERE manga_id = m.id LIMIT 1) as primary_author_id
        FROM mangas m 
        LEFT JOIN publishers p ON m.publisher_id = p.id
        ORDER BY ${orderBy}`;

    db.query(sql, (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        
        // Añadimos el mapeo de la imagen para que Android pueda cargarla
        const processed = results.map(m => ({
            ...m,
            image_url: m.image ? `http://10.0.2.2:3000/imgBBDD/Mangas/${m.image}` : null
        }));
        res.json(processed);
    });
});

// Biblioteca de Mangas del Usuario (CON FIX DEL SERVER ERROR!)
app.get('/api/mangas/library/:userId', verifyToken, (req, res) => {
    const sql = `
        SELECT m.*, ml.status as user_status, ml.volumes_read, ml.personal_rating, p.name as publisher_name,
        (SELECT GROUP_CONCAT(a.name SEPARATOR ', ') 
         FROM manga_authors ma 
         JOIN authors a ON ma.author_id = a.id 
         WHERE ma.manga_id = m.id) as author_names
        FROM mangas_library ml
        JOIN library l ON ml.library_id = l.id
        JOIN mangas m ON ml.manga_id = m.id
        LEFT JOIN publishers p ON m.publisher_id = p.id
        WHERE l.user_id = ?`;

    db.query(sql, [req.params.userId], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        const processed = results.map(m => ({
            ...m,
            image_url: m.image ? `http://10.0.2.2:3000/imgBBDD/${m.image}` : null
        }));
        res.json(processed);
    });
});

// Agregar/Actualizar manga en biblioteca (Auto-crea Library si no existe)
// Add or update manga in library
app.post('/api/mangas/library', verifyToken, (req, res) => {
    const { user_id, manga_id, status, volumes_read, personal_rating } = req.body;

    console.log("--- INTENTO DE GUARDADO DE MANGA ---");
    console.log("Datos:", { user_id, manga_id, status, volumes_read });

    if (!user_id || !manga_id) {
        console.error("ERROR: Faltan IDs obligatorios (manga)");
        return res.status(400).json({ error: "user_id y manga_id son obligatorios" });
    }
    
    // 1. Buscamos si existe la cabecera en 'Library'
    db.query("SELECT id FROM Library WHERE user_id = ?", [user_id], (err, libRes) => {
        if (err) {
            console.error("ERROR SQL (Buscando Library Manga):", err.sqlMessage || err);
            return res.status(500).json({ error: err.message });
        }
        
        const saveManga = (libraryId) => {
            console.log("Usando Library ID para Manga:", libraryId);
            
            // 2. Verificamos si ya existe el manga en la biblioteca de ese usuario
            const checkSql = "SELECT * FROM mangas_library WHERE library_id = ? AND manga_id = ?";
            db.query(checkSql, [libraryId, manga_id], (checkErr, checkRes) => {
                if (checkErr) {
                    console.error("ERROR SQL (Check Manga):", checkErr.sqlMessage || checkErr);
                    return res.status(500).json({ error: checkErr.message });
                }

                if (checkRes.length > 0) {
                    // Si ya existe, actualizamos los datos
                    console.log("Manga existente, actualizando...");
                    const updateSql = "UPDATE mangas_library SET status = ?, volumes_read = ?, personal_rating = ? WHERE library_id = ? AND manga_id = ?";
                    db.query(updateSql, [status, volumes_read || 0, personal_rating || null, libraryId, manga_id], (upErr) => {
                        if (upErr) {
                            console.error("ERROR SQL (Update Manga):", upErr.sqlMessage || upErr);
                            return res.status(500).json({ error: upErr.message });
                        }
                        res.json({ message: "Manga actualizado correctamente" });
                    });
                } else {
                    // Si no existe, insertamos el nuevo registro
                    console.log("Manga nuevo, insertando...");
                    const insertSql = "INSERT INTO mangas_library (library_id, manga_id, status, volumes_read, personal_rating) VALUES (?, ?, ?, ?, ?)";
                    db.query(insertSql, [libraryId, manga_id, status, volumes_read || 0, personal_rating || null], (inErr) => {
                        if (inErr) {
                            console.error("ERROR SQL (Insert Manga):", inErr.sqlMessage || inErr);
                            return res.status(500).json({ error: inErr.message });
                        }
                        res.status(201).json({ message: "Manga guardado correctamente" });
                    });
                }
            });
        };

        if (libRes.length === 0) {
            // 3. Si el usuario no tiene Library (caso raro si ya pasó por juegos, pero posible), se crea
            console.log("Creando nueva Library para el usuario...");
            db.query("INSERT INTO Library (user_id) VALUES (?)", [user_id], (errNew, newLib) => {
                if (errNew) {
                    console.error("ERROR SQL (Creando Library Manga):", errNew.sqlMessage || errNew);
                    return res.status(500).json({ error: errNew.message });
                }
                saveManga(newLib.insertId);
            });
        } else {
            // Ya tiene biblioteca, procedemos al guardado
            saveManga(libRes[0].id);
        }
    });
});


app.get('/api/mangas/:mangaId/reviews', (req, res) => {
    db.query("SELECT * FROM Reviews WHERE manga_id = ?", [req.params.mangaId], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(results);
    });
});

// Detalle de un manga por ID (Con primary_author_id para el botón)
app.get('/api/mangas/:id', (req, res) => {
    const sql = `
        SELECT m.*, p.name as publisher_name, 
        (SELECT GROUP_CONCAT(a.name SEPARATOR ', ') 
         FROM manga_authors ma 
         JOIN authors a ON ma.author_id = a.id 
         WHERE ma.manga_id = m.id) as author_names,
        (SELECT author_id FROM manga_authors WHERE manga_id = m.id LIMIT 1) as primary_author_id
        FROM mangas m 
        LEFT JOIN publishers p ON m.publisher_id = p.id
        WHERE m.id = ?`;

    db.query(sql, [req.params.id], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        if (results.length === 0) return res.status(404).json({ message: "Manga no encontrado" });
        
        const manga = results[0];
        manga.image_url = manga.image ? `http://10.0.2.2:3000/imgBBDD/Mangas/${manga.image}` : null;
            
        res.json(manga);
    });
});

app.post('/api/mangas', verifyToken, (req, res) => {
    const { title, synopsis, release_date, genres, image, demographic } = req.body;
    const sql = "INSERT INTO mangas (title, synopsis, release_date, genres, image, demographic) VALUES (?, ?, ?, ?, ?, ?)";
    db.query(sql, [title, synopsis, release_date, genres, image, demographic || 'Shonen'], (err, result) => {
        if (err) return res.status(500).json({ error: err.message });
        res.status(201).json({ message: "Manga creado", mangaId: result.insertId });
    });
});

app.put('/api/mangas/:id', verifyToken, (req, res) => {
    const { title, synopsis, release_date, genres, image, demographic } = req.body;
    const sql = "UPDATE mangas SET title = ?, synopsis = ?, release_date = ?, genres = ?, image = ?, demographic = ? WHERE id = ?";
    db.query(sql, [title, synopsis, release_date, genres, image, demographic, req.params.id], (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ message: "Manga actualizado" });
    });
});

app.delete('/api/mangas/:id', verifyToken, (req, res) => {
    db.query("DELETE FROM mangas WHERE id = ?", [req.params.id], (err) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({ message: "Manga eliminado" });
    });
});

app.get('/api/authors', (req, res) => {
    db.query("SELECT * FROM authors ORDER BY name ASC", (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(results);
    });
});

app.get('/api/authors/:id', (req, res) => {
    const authorId = req.params.id;
    db.query("SELECT * FROM authors WHERE id = ?", [authorId], (err, authorResults) => {
        if (err || authorResults.length === 0) return res.status(404).json({ message: "Author not found" });

        const author = authorResults[0];
        const sqlMangas = `
            SELECT m.*, CONCAT('http://10.0.2.2:3000/imgBBDD/Mangas/', m.image) as image_url,
            (SELECT GROUP_CONCAT(a.name SEPARATOR ', ') FROM manga_authors ma2 JOIN authors a ON ma2.author_id = a.id WHERE ma2.manga_id = m.id) as author_names
            FROM mangas m 
            JOIN manga_authors ma ON m.id = ma.manga_id 
            WHERE ma.author_id = ?`;
            
        db.query(sqlMangas, [authorId], (err, mangaResults) => {
            if (err) return res.status(500).json({ error: err.message });
            res.json({ ...author, works: mangaResults });
        });
    });
});

app.get('/api/publishers', (req, res) => {
    db.query("SELECT * FROM publishers ORDER BY name ASC", (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json(results);
    });
});

app.get('/api/publishers/:id/mangas', (req, res) => {
    const sql = "SELECT * FROM mangas WHERE publisher_id = ?";
    db.query(sql, [req.params.id], (err, results) => {
        if (err) return res.status(500).json({ error: err.message });
        const processed = results.map(m => ({
            ...m,
            image_url: m.image ? `http://10.0.2.2:3000/imgBBDD/${m.image}` : null
        }));
        res.json(processed);
    });
});

app.listen(3000, () => {
    console.log('Servidor corriendo en puerto 3000');
});