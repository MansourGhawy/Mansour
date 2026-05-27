const { execSync } = require('child_process');
try {
    const out = execSync(process.argv.slice(2).join(' ') || 'git status', { encoding: 'utf-8' });
    console.log(out);
} catch (e) {
    console.error(e.stdout || e.stderr || e.message);
}
